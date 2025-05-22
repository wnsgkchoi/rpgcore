// File: PracticeArena.kt
package org.flash.rpgcore.practice

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.skills.SkillDefs
import org.flash.rpgcore.skills.SkillManager
import org.flash.rpgcore.stats.StatManager
import kotlin.random.Random
import kotlin.math.roundToInt
import java.util.UUID

data class PracticeSession(
    val startLoc: Location,
    val dummy: Zombie,
    val bar: BossBar,
    var ticks: Int = 0
)

object PracticeArena {

    private val ARENA_LOC = Location(Bukkit.getWorld("world_practice")!!, 0.5, -59.0, 0.5)
    private const val START_DELAY_TICK   = 100
    private const val TEST_DURATION_TICK = 1200
    private const val DUMMY_HP           = 100_000_000.0

    internal val sessions   = mutableMapOf<UUID, PracticeSession>()
    private var loopRunning = false
    private val tag         = org.bukkit.NamespacedKey(RPGCore.INSTANCE, "practice_dummy")

    /** 플레이어 입장 */
    fun enter(p: Player) {
        if (sessions.containsKey(p.uniqueId)) {
            p.sendMessage("§c이미 연습 중입니다!")
            return
        }

        // 위치 저장 후 TP
        val back = p.location.clone()
        p.teleport(ARENA_LOC)

        // 더미 소환
        val dummy: Zombie = ARENA_LOC.world.spawn(ARENA_LOC, Zombie::class.java) { z ->
            z.setAI(false)
            z.isCollidable = false
            z.getAttribute(Attribute.MAX_HEALTH)!!.baseValue = DUMMY_HP
            z.health = DUMMY_HP
            z.persistentDataContainer.set(tag, PersistentDataType.BYTE, 1)
        }

        // 보스바
        val bar = Bukkit.createBossBar("피해 측정 준비 중…", BarColor.GREEN, BarStyle.SOLID)
        bar.addPlayer(p)

        sessions[p.uniqueId] = PracticeSession(back, dummy, bar)
        p.sendMessage("§e5초 뒤 측정을 시작합니다!")

        if (!loopRunning) startLoop()
    }

    /** 메인 루프 */
    private fun startLoop() {
        loopRunning = true
        object : BukkitRunnable() {
            override fun run() {
                if (sessions.isEmpty()) {
                    loopRunning = false
                    cancel()
                    return
                }

                val iter = sessions.entries.iterator()
                while (iter.hasNext()) {
                    val (uuid, ses) = iter.next()
                    val p = Bukkit.getPlayer(uuid) ?: continue
                    ses.ticks++

                    // 더미 리스폰
                    if (!ses.dummy.isValid) {
                        ses.dummy.remove()
                        ses.dummy.teleport(ARENA_LOC)
                    }

                    val dealt = DUMMY_HP - ses.dummy.health
                    when (ses.ticks) {
                        START_DELAY_TICK -> p.sendMessage("§a측정 시작! (60초)")
                        in (START_DELAY_TICK + 1) until (START_DELAY_TICK + TEST_DURATION_TICK) -> {
                            val remain = (START_DELAY_TICK + TEST_DURATION_TICK - ses.ticks) / 20
                            val progress = (dealt / DUMMY_HP).coerceAtMost(1.0)
                            ses.bar.progress = progress
                            ses.bar.setTitle(
                                "§e남은 시간: §f${remain}초 §7| §e누적 피해: §f${dealt.roundToInt()}"
                            )
                        }
                        START_DELAY_TICK + TEST_DURATION_TICK -> {
                            finish(p, dealt)
                            ses.dummy.remove()
                            ses.bar.removeAll()
                            iter.remove()
                        }
                    }
                }
            }
        }.runTaskTimer(RPGCore.INSTANCE, 1L, 1L)
    }

    /** 측정 종료 처리 */
    private fun finish(p: Player, total: Double) {
        // 타이틀
        p.showTitle(
            Title.title(
                Component.text("60초 누적 피해", NamedTextColor.GOLD),
                Component.text(total.roundToInt().toString(), NamedTextColor.YELLOW),
                Title.Times.times(
                    java.time.Duration.ZERO,
                    java.time.Duration.ofSeconds(3),
                    java.time.Duration.ofSeconds(2)
                )
            )
        )

        // 보상: 일정 피해 이상 시 1% 확률로 스킬 습득 (장착 조건 무관)
        if (total >= 10000 && Random.nextDouble() < 0.01) {
            val allDefs = SkillDefs.all().toList()
            if (allDefs.isNotEmpty()) {
                val def = allDefs.random()
                // 스킬 학습
                SkillManager.learnSkill(p.uniqueId, def.id)
                // 빈 슬롯 있으면 자동 장착
                val skills = SkillManager.getSkills(p.uniqueId)
                val freeSlot = (1..3).firstOrNull { skills.get(it) == null }
                if (freeSlot != null) {
                    SkillManager.updateSkill(p.uniqueId, freeSlot, def.id, 1)
                }
                p.sendMessage("§d연습 보상으로 §f[${def.displayName}]§d 스킬을 습득했습니다!")
                p.playSound(p.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f)
            }
        }

        // 원위치 TP 및 경험치
        p.teleport(sessions[p.uniqueId]!!.startLoc)
        p.giveExp((total / 30).toInt())
    }
}

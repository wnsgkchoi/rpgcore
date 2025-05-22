package org.flash.rpgcore.sidebar

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.DisplaySlot
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.classs.PlayerClass
import org.flash.rpgcore.skills.SkillDefs
import org.flash.rpgcore.skills.SkillManager
import org.flash.rpgcore.skills.CooldownManager
import org.flash.rpgcore.stats.StatManager
import org.flash.rpgcore.util.VirtualHp
import org.flash.rpgcore.util.VirtualMp
import kotlin.math.sqrt

object SidebarService {
    // § 코드를 파싱할 시리얼라이저
    private val LEGACY = LegacyComponentSerializer.legacySection()

    /** 초기화: 주기 갱신 + 체력 변화 리스너 등록 */
    fun init(p: Plugin) {
        Bukkit.getOnlinePlayers().forEach { player ->
            VirtualHp.fillMax(player)
            VirtualMp.fillMax(player)
            StatManager.updatePlayerAttributes(player)
            StatManager.recalcFor(player)
            update(player)
        }
        Bukkit.getScheduler().runTaskTimer(
            p,
            Runnable { Bukkit.getOnlinePlayers().forEach { update(it) } },
            0L, 200L
        )
        p.server.pluginManager.registerEvents(HealthListener, p)
    }

    /** 즉시 사이드바 갱신 */
    fun updateNow(player: Player) = update(player)

    private fun update(p: Player) {
        // 기존 메인 스코어보드가 아니면 새로 만들기
        val board = p.scoreboard.takeIf { it !== Bukkit.getScoreboardManager().mainScoreboard }
            ?: Bukkit.getScoreboardManager().newScoreboard.also { p.scoreboard = it }

        // Objective 가져오거나, §6§lRPG 제목으로 새로 등록
        val obj = board.getObjective("rpg")
            ?: board.registerNewObjective(
                "rpg",
                "dummy",
                LEGACY.deserialize("§6§lRPG")
            )
        obj.displaySlot = DisplaySlot.SIDEBAR

        val usedEntries = mutableSetOf<String>()
        fun line(score: Int, text: String) {
            val entry = "§$score"
            usedEntries += entry
            val team = board.getTeam("l$score") ?: board.registerNewTeam("l$score").apply {
                addEntry(entry)
            }
            // § 컬러코드를 Component 로 파싱
            team.prefix(LEGACY.deserialize(text))
            obj.getScore(entry).score = score
        }

        val stats = StatManager.load(p.uniqueId)
        val cls   = ClassManager.get(p.uniqueId)
        val exp   = p.totalExperience

        // 장착 스킬
        val skills       = SkillManager.getSkills(p.uniqueId)
        val skill1Entry  = skills.slot1
        val skill2Entry  = skills.slot2
        val skill3Entry  = skills.slot3

        fun formatSkill(entry: Pair<String, Int>?): String {
            if (entry == null) return "없음"
            val (id, _) = entry
            val name = SkillDefs.get(id)?.displayName ?: "알 수 없음"
            val cd   = CooldownManager.getRemainingSeconds(p.uniqueId, id)
            return if (cd > 0) "$name (${cd}s)" else name
        }

        val skill1Display = formatSkill(skill1Entry)
        val skill2Display = formatSkill(skill2Entry)
        val skill3Display = formatSkill(skill3Entry)

        val hpNow = p.health.toInt() + VirtualHp.get(p)
        val hpMax = stats.finalHP
        val mpNow = VirtualMp.get(p)
        val mpMax = stats.finalMP

        val lost  = (hpMax - hpNow).toDouble()
        val bonus = if (cls == PlayerClass.BERSERKER) (lost * sqrt(lost) * 0.02).toInt() else 0
        val attackLine = if (bonus > 0)
            "§c공격력: ${stats.finalAttack.toInt()} §6(+$bonus)"
        else
            "§c공격력: ${stats.finalAttack.toInt()}"

        line(10, "§6클래스: ${cls.display}")
        line(9,  "§4체력:  $hpNow / ${hpMax.toInt()}")
        line(8,  "§9MP:    ${mpNow.toInt()} / ${mpMax.toInt()}")
        line(7,  attackLine)
        line(6,  "§a방어력: ${stats.finalDefense.toInt()}")
        line(5,  "§b마력:  ${stats.finalMagic.toInt()}")
        line(4,  "§d마방:  ${stats.finalMdef.toInt()}")
        line(3,  "§f스킬1: $skill1Display")
        line(2,  "§f스킬2: $skill2Display")
        line(1,  "§f스킬3: $skill3Display")
        line(0,  "§e경험치: $exp")

        // 남은 unused entry 정리
        board.entries.filter { it !in usedEntries }.forEach { entry ->
            obj.getScore(entry).resetScore()
            board.getTeam("l${entry.trimStart('§')}")?.unregister()
        }
    }

    private object HealthListener : Listener {
        @EventHandler
        fun onDamage(e: EntityDamageEvent) {
            (e.entity as? Player)?.let { updateNow(it) }
        }
    }
}

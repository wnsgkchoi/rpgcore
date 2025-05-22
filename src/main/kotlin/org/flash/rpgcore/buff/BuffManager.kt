// src/main/kotlin/org/flash/rpgcore/buff/BuffManager.kt
package org.flash.rpgcore.buff

import org.bukkit.scheduler.BukkitRunnable
import org.flash.rpgcore.RPGCore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ──────────────────────────────────────────────────────────
 * ■ 버프 관리
 *   - 플레이어(UUID) -> (버프 ID -> Buff) 구조
 *   - tick(20/s) 단위로 지속시간 감소 → 0 이면 자동 만료
 *   - 쿨타임 없음, durationSec 으로만 관리
 * ──────────────────────────────────────────────────────────
 */
object BuffManager {

    private data class Buff(
        var coef: Double,
        var ticksLeft: Int          // 남은 tick (20tick = 1초)
    )

    /** playerUUID -> (buffId -> Buff) */
    private val buffMap: MutableMap<UUID, MutableMap<String, Buff>> = ConcurrentHashMap()

    /**
     * 버프 활성화 (동일 id 재사용 시 갱신)
     */
    fun activate(uuid: UUID, id: String, coef: Double, durationSec: Int) {
        val playerBuffs = buffMap.computeIfAbsent(uuid) { ConcurrentHashMap() }
        playerBuffs[id] = Buff(coef, durationSec * 20)
    }

    /**
     * 해당 버프 유지 중인가?
     */
    fun isActive(uuid: UUID, id: String): Boolean =
        buffMap[uuid]?.get(id)?.ticksLeft?.let { it > 0 } ?: false

    /**
     * 버프 계수 가져오기(없으면 0)
     */
    fun coef(uuid: UUID, id: String): Double =
        buffMap[uuid]?.get(id)?.coef ?: 0.0

    /**
     * 메인 플러그인 onEnable 에서 호출하세요.
     *
     *     BuffManager.startTask(this)
     */
    fun startTask(plugin: RPGCore) {
        object : BukkitRunnable() {
            override fun run() {
                // 모든 버프 tick 감소 및 만료 처리
                val itPlayer = buffMap.entries.iterator()
                while (itPlayer.hasNext()) {
                    val entry = itPlayer.next()
                    val buffs = entry.value
                    val itBuff = buffs.entries.iterator()
                    while (itBuff.hasNext()) {
                        val buff = itBuff.next().value
                        buff.ticksLeft--
                        if (buff.ticksLeft <= 0) itBuff.remove()
                    }
                    if (buffs.isEmpty()) itPlayer.remove()
                }
            }
        }.runTaskTimer(plugin, 1L, 1L)   // 매 tick(50 ms)마다
    }
}

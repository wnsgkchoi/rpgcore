package org.flash.rpgcore.util

import org.bukkit.entity.Player
import org.flash.rpgcore.stats.StatManager
import java.util.UUID

object VirtualMp {
    private val currentMp = mutableMapOf<UUID, Int>()

    /** 현재 MP 조회 (없으면 최종 MP로 초기화) */
    fun get(p: Player): Int =
        currentMp.getOrPut(p.uniqueId) { StatManager.load(p.uniqueId).finalMP.toInt() }

    /**
     * MP 추가 (maxMp 매개변수 대신 항상 최종 MP로 캡)
     */
    fun add(p: Player, amount: Int) {
        val maxMp = StatManager.load(p.uniqueId).finalMP.toInt()
        currentMp[p.uniqueId] = (get(p) + amount).coerceAtMost(maxMp)
    }

    /** MP 차감 (0 미만 방지) */
    fun subtract(p: Player, amount: Int) {
        val now = get(p) - amount
        currentMp[p.uniqueId] = now.coerceAtLeast(0)
    }

    /** 현재 MP를 최대 MP로 채움 (로그인·레벨업 시) */
    fun fillMax(p: Player) {
        currentMp[p.uniqueId] = StatManager.load(p.uniqueId).finalMP.toInt()
    }
}

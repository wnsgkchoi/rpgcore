// File: CooldownManager.kt
package org.flash.rpgcore.skills

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 플레이어(UUID) × 스킬 ID별로 만료 시각(epoch ms)을 저장합니다.
 */
object CooldownManager {
    private val cooldowns = ConcurrentHashMap<UUID, MutableMap<String, Long>>()

    /**
     * 해당 스킬을 durationSec(초)만큼 쿨다운에 등록
     */
    fun setCooldown(uuid: UUID, skillId: String, durationSec: Double) {
        val expiresAt = System.currentTimeMillis() + (durationSec * 1000).toLong()
        val map = cooldowns.computeIfAbsent(uuid) { ConcurrentHashMap() }
        map[skillId] = expiresAt
    }

    /**
     * 쿨타임이 남아 있으면 true
     */
    fun isCooling(uuid: UUID, skillId: String): Boolean {
        val expiry = cooldowns[uuid]?.get(skillId) ?: return false
        return System.currentTimeMillis() < expiry
    }

    /**
     * 남은 쿨타임(초)을 반환. 쿨타임이 없거나 만료되었으면 0
     */
    fun getRemainingSeconds(uuid: UUID, skillId: String): Int {
        val expiry = cooldowns[uuid]?.get(skillId) ?: return 0
        val remMs = expiry - System.currentTimeMillis()
        return if (remMs > 0) ((remMs + 999) / 1000).toInt() else 0
    }
}

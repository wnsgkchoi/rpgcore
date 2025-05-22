package org.flash.rpgcore.combat

import org.bukkit.entity.Player
import org.flash.rpgcore.equipment.EquipmentDef
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object WeaponAttackCooldownManager {
    // 플레이어 UUID -> 만료 시각 (epoch ms)
    private val attackCooldowns = ConcurrentHashMap<UUID, Long>()

    /**
     * 해당 플레이어의 일반 공격 쿨타임을 설정합니다.
     * @param player 대상 플레이어
     * @param weaponDef 사용된 무기의 EquipmentDef (cooldownMs를 가져오기 위함)
     */
    fun setCooldown(player: Player, weaponDef: EquipmentDef) {
        if (weaponDef.cooldownMs <= 0) return // 쿨타임이 0 이하면 설정하지 않음

        val expiresAt = System.currentTimeMillis() + weaponDef.cooldownMs
        attackCooldowns[player.uniqueId] = expiresAt
    }

    /**
     * 플레이어가 현재 일반 공격 쿨타임 중인지 확인합니다.
     * @param player 대상 플레이어
     * @return 쿨타임 중이면 true, 아니면 false
     */
    fun isCooling(player: Player): Boolean {
        val expiry = attackCooldowns[player.uniqueId] ?: return false
        return System.currentTimeMillis() < expiry
    }

    /**
     * 남은 일반 공격 쿨타임(초)을 반환합니다. 쿨타임이 없거나 만료되었으면 0을 반환합니다.
     * @param player 대상 플레이어
     * @return 남은 쿨타임(초)
     */
    fun getRemainingSeconds(player: Player): Int {
        val expiry = attackCooldowns[player.uniqueId] ?: return 0
        val remainingMs = expiry - System.currentTimeMillis()
        return if (remainingMs > 0) ((remainingMs + 999) / 1000).toInt() else 0
    }

    /**
     * (선택적) 플레이어 퇴장 시 쿨다운 정보 제거
     */
    fun clearCooldown(player: Player) {
        attackCooldowns.remove(player.uniqueId)
    }
}
package org.flash.rpgcore.combat

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

/**
 * 플레이어가 어떤 피해를 받든지 한 번 맞으면
 * 무적 틱을 설정해서 다음 피해를 1초간 막아줍니다.
 */
class UniversalInvincibilityListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAnyDamage(e: EntityDamageEvent) {
        val p = e.entity as? Player ?: return
        // 한 번 피격되면 곧바로 20 틱 = 1초 무적
        p.noDamageTicks = p.maximumNoDamageTicks
    }
}

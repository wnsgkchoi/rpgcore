package org.flash.rpgcore.combat

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRegainHealthEvent

/**
 * 바닐라 ‘포화(허기) 회복’을 전부 무효화한다.
 * - 우리는 별도 태스크로 10초마다 5HP 회복을 처리함
 */
class NaturalRegenBlocker : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onRegen(e: EntityRegainHealthEvent) {
        if (e.regainReason == EntityRegainHealthEvent.RegainReason.SATIATED) {
            e.isCancelled = true
        }
    }
}

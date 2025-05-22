package org.flash.rpgcore.listener

import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemDamageEvent

/**
 * 모든 도구(곡괭이, 도끼, 삽, 괭이)의 내구도 감소를 방지합니다.
 */
class ToolDurabilityListener : Listener {
    @EventHandler
    fun onItemDamage(e: PlayerItemDamageEvent) {
            e.isCancelled = true
    }
}

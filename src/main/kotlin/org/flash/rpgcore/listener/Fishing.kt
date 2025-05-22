package org.flash.rpgcore.listener

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.map.MapCursor

class Fishing: Listener {

    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        // Only trigger when catching fish/item
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return
        };
        event.hook.remove()
        event.caught?.remove()
        event.expToDrop = 0
    }
}
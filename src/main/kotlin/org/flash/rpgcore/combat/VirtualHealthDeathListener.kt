package org.flash.rpgcore.combat

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.flash.rpgcore.util.VirtualHp
import kotlin.math.ceil

class VirtualHealthDeathListener : Listener {
    /**
     * 모든 대미지 처리 후, 실제 체력(p.health) + 버퍼(VirtualHp.get) 총합이 0 이하면
     * 강제로 p.health=0 을 설정해 플레이어 사망을 트리거합니다.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAnyDamage(e: EntityDamageEvent) {
        val p = e.entity as? Player ?: return

        // (1) 엔진 체력(실제 p.health)
        val realHp = ceil(p.health).toInt()
        // (2) 가상 버퍼 HP
        val buffer = VirtualHp.get(p)
        // (3) 총합 계산
        val totalHp = realHp + buffer

        if (totalHp <= 0) {
            // 버퍼까지 다 소진됐으니, 진짜 죽인다
            p.health = 0.0
        }
    }
}

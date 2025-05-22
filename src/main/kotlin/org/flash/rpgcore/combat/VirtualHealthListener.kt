package org.flash.rpgcore.combat

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.entity.Player
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.util.VirtualHp
import kotlin.math.min

/**
 * ──────────────────────────────────────────────────────────────
 *  • 스탯(virtual) 체력 → 버퍼로 사용
 *  • 이벤트 재호출 없이, 한 번의 EntityDamageEvent 내에서 처리
 * ──────────────────────────────────────────────────────────────
 */
class VirtualHealthListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamage(e: EntityDamageEvent) {
        val p = e.entity as? Player ?: return
        var remaining = e.finalDamage

        /* ---------- 가상 버퍼 소모 ---------- */
        val buf = VirtualHp.get(p)
        if (buf > 0) {
            val after = buf - remaining
            if (after >= 0) {                    // 버퍼로 모두 흡수
                VirtualHp.set(p, after.toInt())
                e.isCancelled = true
                remaining = 0.0
            } else {                            // 버퍼 소진, 일부 실체력으로
                VirtualHp.set(p, 0)
                remaining = -after
            }
        }

        /* ---------- 실체력 피해량 확정 ---------- */
        if (remaining > 0) {
            val real = min(p.health, remaining)
            e.damage = real                     // 재귀 호출 방지 – damage() 사용 X
        }

        SidebarService.updateNow(p)
    }
}

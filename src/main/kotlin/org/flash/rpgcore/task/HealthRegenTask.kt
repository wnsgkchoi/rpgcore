package org.flash.rpgcore.task

import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.stats.StatManager
import org.flash.rpgcore.stats.Stats
import org.flash.rpgcore.util.VirtualHp
import kotlin.math.ceil
import kotlin.math.min

object HealthRegenTask {

    fun start(plugin: Plugin) {
        // 20틱(1초)마다 회복 실행
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            Bukkit.getOnlinePlayers().forEach { player ->
                regenOnce(player)
            }
        }, 100L, 100L)
    }

    private fun regenOnce(p: Player) {
        val stats = StatManager.load(p.uniqueId)
        val maxTotalHp = stats.finalHP                           // ▶ 장비 포함 최종 HP
        val currentVanilla = p.health                            // 0.0..vanillaMax
        val currentVirtual = VirtualHp.get(p)                    // 0..(finalHP-vanillaMax)

        val totalNow = currentVanilla + currentVirtual           // 현재 총 HP
        if (totalNow >= maxTotalHp) return                       // 이미 가득 찼다면 패스

        // 회복량: 예로 “최대 HP의 2%”를 매초 회복하도록 (원하시면 고정값으로 변경 가능)
        val regenAmount = ceil(maxTotalHp * 0.02).coerceAtLeast(1.0)

        // 최대를 넘지 않는 선에서 회복
        val totalAfter = min(totalNow + regenAmount, maxTotalHp)

        // vanilla 체력으로 채우고, 넘는 부분은 가상 HP 버퍼로
        val vanillaCap = p.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        val newVanilla = min(totalAfter, vanillaCap)
        p.health = newVanilla

        val newVirtual = (totalAfter - newVanilla).toInt()
        VirtualHp.set(p, newVirtual)

        SidebarService.updateNow(p)
    }
}

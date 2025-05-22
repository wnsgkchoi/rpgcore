package org.flash.rpgcore.task

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.stats.StatManager
import org.flash.rpgcore.util.VirtualMp

object MpRegenTask {
    fun start(plugin: Plugin) {
        // 20틱(1초)마다 각 플레이어 MP 리젠
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            Bukkit.getOnlinePlayers().forEach { player ->
                // 1) 회복량 계산 (예: 매초 3)
                val regenAmount = (StatManager.load(player.uniqueId).finalMP * 0.05).toInt()

                // 2) 장비 포함한 최종 MP 기준으로 add
                VirtualMp.add(player, regenAmount)

                // 3) 사이드바 갱신
                SidebarService.updateNow(player)
            }
        }, 20L, 20L)
    }
}

// src/main/kotlin/org/flash/rpgcore/command/ReloadCommand.kt
package org.flash.rpgcore.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.equipment.EquipmentDefs
import org.flash.rpgcore.equipment.RecipeDefs
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.skills.SkillDefs
import org.flash.rpgcore.skills.SkillManager
import org.flash.rpgcore.stats.StatManager

class ReloadCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (args.isEmpty() || args[0].lowercase() != "reload") return false
        if (!sender.hasPermission("rpgcore.op")) {
            sender.sendMessage("§c권한이 없습니다")
            return true
        }

        val t0 = System.currentTimeMillis()
        sender.sendMessage("§7[RPGCore] 리로드 시작…")

        /* ① YML & 레지스트리 다시 읽기 */
        EquipmentDefs.reload()
        RecipeDefs.load()
        SkillDefs.reload()

        SkillManager.reloadAllPlayers()

        /* ② 모든 온라인 플레이어 스탯 재계산 */
        Bukkit.getOnlinePlayers().forEach {
            StatManager.clear(it.uniqueId)
            StatManager.load(it.uniqueId)
            StatManager.recalcFor(it)
            SidebarService.updateNow(it)
            SkillManager.getSkills(it.uniqueId)
        }

        val dt = System.currentTimeMillis() - t0
        sender.sendMessage("§a[RPGCore] 리로드 완료 (${dt}ms)")
        return true
    }
}

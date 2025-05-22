package org.flash.rpgcore.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.flash.rpgcore.skills.SkillDefs

class ListSkillsCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        val defs = SkillDefs.all().sortedBy { it.id }
        if (defs.isEmpty()) {
            sender.sendMessage("§c로드된 스킬이 없습니다.")
            return true
        }
        sender.sendMessage("§6=== 로드된 스킬 목록 (${defs.size}) ===")
        defs.forEach { def ->
            sender.sendMessage("§e- ${def.id} §7: ${def.displayName} (MaxLv: ${def.maxLevel})")
        }
        return true
    }
}
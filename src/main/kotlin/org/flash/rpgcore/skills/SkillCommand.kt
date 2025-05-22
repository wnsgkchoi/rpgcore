// File: SkillCommand.kt
package org.flash.rpgcore.skills

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * /skill 명령으로 스킬 장착 GUI를 엽니다.
 */
class SkillCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return true
        }
        SkillEquipGUI.open(sender)
        return true
    }
}

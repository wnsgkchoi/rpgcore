package org.flash.rpgcore.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.flash.rpgcore.practice.PracticeArena

class PracticeCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender, cmd: Command, label: String, args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("플레이어만 사용할 수 있습니다")
            return true
        }
        PracticeArena.enter(sender)
        return true
    }
}

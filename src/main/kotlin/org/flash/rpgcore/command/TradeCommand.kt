// src/main/kotlin/org/flash/rpgcore/command/TradeCommand.kt
package org.flash.rpgcore.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.flash.rpgcore.trade.TradeManager

class TradeCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§e사용법: /trade request|accept|cancel <플레이어>")
            return true
        }
        when (args[0].lowercase()) {
            "request" -> {
                if (args.size != 2) {
                    sender.sendMessage("§e사용법: /trade request <플레이어>")
                    return true
                }
                val target = Bukkit.getPlayerExact(args[1])
                if (target == null) {
                    sender.sendMessage("§c플레이어를 찾을 수 없습니다.")
                    return true
                }
                if (!TradeManager.request(sender, target)) {
                    sender.sendMessage("§c이미 거래 중이거나 요청할 수 없습니다.")
                } else {
                    sender.sendMessage("§a${target.name}님에게 거래 요청을 보냈습니다.")
                    target.sendMessage("§e${sender.name}님이 거래를 요청했습니다. /trade accept ${sender.name} 으로 수락하세요.")
                }
            }
            "accept" -> {
                if (args.size != 2) {
                    sender.sendMessage("§e사용법: /trade accept <플레이어>")
                    return true
                }
                val requester = Bukkit.getPlayerExact(args[1])
                if (requester == null) {
                    sender.sendMessage("§c플레이어를 찾을 수 없습니다.")
                    return true
                }
                if (!TradeManager.accept(sender, requester)) {
                    sender.sendMessage("§c수락 가능한 거래 요청이 없습니다.")
                }
            }
            "cancel" -> {
                TradeManager.cancel(sender)
            }
            else -> return false
        }
        return true
    }
}

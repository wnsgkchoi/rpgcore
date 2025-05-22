package org.flash.rpgcore.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.flash.rpgcore.equipment.EquipmentDefs
import org.flash.rpgcore.equipment.ItemFactory

class GiveEquipCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("rpgcore.giveequip")) {
            sender.sendMessage("§cYou do not have permission.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§eUsage: /giveequip <equip_id> [level] [player]")
            return true
        }

        val equipId = args[0]
        val def = EquipmentDefs.get(equipId)
        if (def == null) {
            sender.sendMessage("§cUnknown equipment ID: $equipId")
            return true
        }

        // level 파싱 (기본 0)
        val level = args.getOrNull(1)?.toIntOrNull()?.coerceIn(0, def.maxEnhance)
            ?: 0

        // 대상 플레이어 파싱 (기본 명령어 보낸 사람)
        val target: Player? = args.getOrNull(2)
            ?.let { Bukkit.getPlayerExact(it) }
            ?: (sender as? Player)

        if (target == null || !target.isOnline) {
            sender.sendMessage("§cPlayer not found or not online.")
            return true
        }

        // 아이템 생성 및 지급
        val item = ItemFactory.create(def, level)
        target.inventory.addItem(item)
        sender.sendMessage("§aGave §f${def.displayName} §alevel §f$level §ato §f${target.name}")

        return true
    }
}
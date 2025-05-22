// File: GiveSkillCommand.kt
package org.flash.rpgcore.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.skills.SkillDefs
import org.flash.rpgcore.skills.SkillManager

/**
 * /giveskill <skillId> [level] [slot]
 *
 * - skillId: SkillDefs.yml 에 정의된 ID
 * - level:   (Optional) 장착 레벨. 지정하지 않으면 1
 * - slot:    (Optional) 1~3 중 장착할 슬롯. 지정하지 않으면 빈 슬롯에 자동 장착, 없으면 학습만
 */
class GiveSkillCommand : CommandExecutor {
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
            sender.sendMessage("§c사용법: /giveskill <skillId> [level] [slot]")
            return true
        }

        val player   = sender as Player
        val uuid     = player.uniqueId
        val rawId    = args[0]
        val def      = SkillDefs.get(rawId)
        if (def == null) {
            player.sendMessage("§c유효하지 않은 스킬 ID: $rawId")
            return true
        }

        val level = args.getOrNull(1)?.toIntOrNull() ?: 1
        if (level !in 1..def.maxLevel) {
            player.sendMessage("§c레벨은 1~${def.maxLevel} 사이여야 합니다.")
            return true
        }

        // 1) 항상 학습 처리
        SkillManager.learnSkill(uuid, def.id)
        player.sendMessage("§a스킬 §f[${def.displayName}]§a(레벨 $level) 학습 완료")

        // 2) 장착 시도: 플레이어 클래스가 허용된 스킬만
        val playerClass = ClassManager.get(uuid)
        if (!def.allowedClasses.contains(playerClass)) {
            player.sendMessage("§c이 스킬은 당신의 클래스(${playerClass.display})에서 사용할 수 없습니다. 장착하지 않습니다.")
            return true
        }

        // 슬롯 인자 처리
        val slotArg = args.getOrNull(2)?.toIntOrNull()
        if (slotArg == null) {
            // 빈 슬롯에 자동 장착
            val ps   = SkillManager.getSkills(uuid)
            val free = (1..3).firstOrNull { ps.get(it) == null }
            if (free != null) {
                SkillManager.updateSkill(uuid, free, def.id, level)
                player.sendMessage("§a자동 장착 ▶ 슬롯 $free 에 [$rawId] 레벨 $level")
            } else {
                player.sendMessage("§c빈 슬롯이 없어 자동 장착할 수 없습니다. `/skill` 에서 교체하세요.")
            }
            return true
        }

        // 지정 슬롯
        if (slotArg !in 1..3) {
            player.sendMessage("§c슬롯 번호는 1~3 사이여야 합니다.")
            return true
        }
        SkillManager.updateSkill(uuid, slotArg, def.id, level)
        player.sendMessage("§a장착 ▶ 슬롯 $slotArg 에 [$rawId] 레벨 $level")
        return true
    }
}

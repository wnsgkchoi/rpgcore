package org.flash.rpgcore.skills

import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.entity.Player
import org.flash.rpgcore.classs.ClassManager
import kotlin.random.Random

/**
 * SkillEquipGUI(장착)와 SkillSelectGUI(선택) 클릭을 처리합니다.
 */
class SkillListener : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onClick(e: InventoryClickEvent) {
        val player = e.whoClicked as? Player ?: return

        // --- 1) 장착 GUI 처리 ---
        if ((e.view.title() as? TextComponent)?.content() == SkillEquipGUI.TITLE) {
            if (e.clickedInventory != e.view.topInventory) return
            e.isCancelled = true

            val idx  = e.slot
            val slot = idx / 3 + 1    // 0~2 → slot1, 3~5 → slot2, ...
            val type = idx % 3        // 0=info,1=enhance,2=change

            when (type) {
                0 -> { /* 정보 슬롯 클릭: 아무 동작 없음 */ }
                1 -> {  // 강화 버튼
                    SkillManager.getSkills(player.uniqueId)
                        .get(slot)
                        ?.let { (skillId, lvl) ->
                            enhanceSkill(player, skillId, lvl) { newLvl ->
                                SkillManager.updateSkill(player.uniqueId, slot, skillId, newLvl)
                            }
                        }
                    SkillEquipGUI.open(player)
                }
                2 -> {  // 교체 버튼
                    SkillSelectGUI.open(player, slot)
                }
            }
            return
        }

        // 2) 스킬 선택 GUI
        val title = e.view.title()
        if (title is TextComponent && title.content() == SkillSelectGUI.TITLE) {
            if (e.clickedInventory != e.view.topInventory) return
            e.isCancelled = true

            val uuid      = player.uniqueId
            val available = SkillSelectGUI.popSessionAvailable(uuid) ?: return
            val idx       = e.slot
            if (idx < 0 || idx >= available.size) return

            val def       = available[idx]
            val prevLevel = SkillManager.getSkills(uuid)
                .learned.getOrDefault(def.id, 1)

            SkillSelectGUI.popSessionSlot(uuid)?.let { slotIndex ->
                SkillManager.updateSkill(uuid, slotIndex, def.id, prevLevel)
            }

            SkillEquipGUI.open(player)
        }
    }

    /** 공통 강화 로직 */
    private fun enhanceSkill(
        player: Player,
        skillId: String,
        curLevel: Int,
        applyLevel: (newLevel: Int) -> Unit
    ) {
        val def = SkillDefs.get(skillId)
            ?: return player.sendActionBar("§c존재하지 않는 스킬입니다.")

        if (curLevel >= def.maxLevel) {
            return player.sendActionBar("§e이미 최대 레벨입니다.")
        }

        val cost   = def.getXpCost(curLevel + 1)
        val chance = def.getSucRate(curLevel + 1)

        if (player.totalExperience < cost) {
            return player.sendActionBar("§cXP가 부족합니다 (필요 $cost)")
        }

        player.giveExp(-cost)
        if (Random.nextDouble(0.0, 100.0) < chance) {
            val newLvl = curLevel + 1
            applyLevel(newLvl)
            player.sendActionBar("§a강화 성공! 레벨 $newLvl → 계수 ${def.getAtkCoef(newLvl)}")
        } else {
            player.sendActionBar("§c강화 실패…")
        }
    }
}

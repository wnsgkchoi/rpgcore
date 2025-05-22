package org.flash.rpgcore.skills

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.classs.PlayerClass // PlayerClass 임포트 확인
import org.flash.rpgcore.equipment.EquipManager
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.util.VirtualMp
import org.flash.rpgcore.util.WeaponRules

class SkillUseListener : Listener {

    @EventHandler
    fun onRightClick(e: PlayerInteractEvent) {
        val p = e.player
        if (e.hand != EquipmentSlot.HAND) {
            return
        }

        val playerClass = ClassManager.get(p.uniqueId)
        // NOVICE는 스킬을 사용할 수 없음
        if (playerClass == PlayerClass.NOVICE) {
            return
        }

        val itemInHand = p.inventory.itemInMainHand
        val equippedRpgWeapon = EquipManager.getSlots(p.uniqueId).weapon

        if (!WeaponRules.canPerformAction(playerClass, itemInHand, equippedRpgWeapon)) {
            return
        }

        if (e.action == Action.RIGHT_CLICK_AIR || e.action == Action.RIGHT_CLICK_BLOCK) {
            e.isCancelled = true
            useSkillSlot(p, 1, equippedRpgWeapon)
        }
    }

    @EventHandler
    fun onSwapHand(e: PlayerSwapHandItemsEvent) {
        val p = e.player
        val playerClass = ClassManager.get(p.uniqueId)
        // NOVICE는 스킬을 사용할 수 없음
        if (playerClass == PlayerClass.NOVICE) {
            e.isCancelled = true // 스킬 사용 시도가 스왑을 유발하지 않도록 명시적 취소
            // p.sendActionBar(...) // 필요시 메시지
            return
        }

        val itemInHand = p.inventory.itemInMainHand
        val equippedRpgWeapon = EquipManager.getSlots(p.uniqueId).weapon

        if (!WeaponRules.canPerformAction(playerClass, itemInHand, equippedRpgWeapon)) {
            e.isCancelled = true
            return
        }

        e.isCancelled = true
        if (p.isSneaking) {
            useSkillSlot(p, 3, equippedRpgWeapon)
        } else {
            useSkillSlot(p, 2, equippedRpgWeapon)
        }
    }

    private fun useSkillSlot(p: Player, slot: Int, equippedRpgWeapon: ItemStack?) {
        // NOVICE는 이 함수가 호출되기 전에 이미 차단되었어야 함.
        // (이중 안전장치로 여기서도 체크할 수 있으나, 이벤트 핸들러에서 막는 것이 더 효율적)

        if (ClassManager.get(p.uniqueId) != PlayerClass.NOVICE && (equippedRpgWeapon == null || equippedRpgWeapon.type.isAir)) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("§c장착된 무기가 없어 스킬을 사용할 수 없습니다."))
            return
        }

        val skillEntry = SkillManager.getSkills(p.uniqueId).get(slot)
        if (skillEntry == null) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("§c${slot}번 슬롯에 장착된 스킬이 없습니다."))
            return
        }
        val skillId: String = skillEntry.first
        val currentSkillLevel: Int = skillEntry.second

        val skillDef = SkillDefs.get(skillId)
        if (skillDef == null) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("§c알 수 없는 스킬입니다: $skillId"))
            return
        }

        // SkillDefinition의 allowedClasses 검사는 여전히 유효 (NOVICE가 아닌 다른 클래스용)
        val playerClass = ClassManager.get(p.uniqueId) // 이미 위에서 가져왔지만, 명확성을 위해 다시 가져오거나 파라미터로 전달 가능
        if (skillDef.allowedClasses.isNotEmpty() && playerClass !in skillDef.allowedClasses) {
            // NOVICE는 어차피 이전에 차단되므로, 이 조건은 NOVICE가 아닌 다른 클래스가
            // 부적절한 스킬을 (어떤 이유로든) 장착했을 경우를 대비함.
            p.sendActionBar(net.kyori.adventure.text.Component.text("§c현재 클래스로는 사용할 수 없는 스킬입니다."))
            return
        }

        val requiredMp = skillDef.getMpCost(currentSkillLevel)
        if (VirtualMp.get(p) < requiredMp) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("§cMP가 부족합니다. (필요 MP: $requiredMp)"))
            return
        }

        if (CooldownManager.isCooling(p.uniqueId, skillId)) {
            val remaining = CooldownManager.getRemainingSeconds(p.uniqueId, skillId)
            p.sendActionBar(net.kyori.adventure.text.Component.text("§c쿨타임: ${remaining}초"))
            return
        }

        VirtualMp.subtract(p, requiredMp)
        CooldownManager.setCooldown(p.uniqueId, skillId, skillDef.getCooldown(currentSkillLevel))

        equippedRpgWeapon?.let { SkillEffects.executeEffect(p, skillDef, currentSkillLevel, it) }

        p.sendActionBar(net.kyori.adventure.text.Component.text("§b${skillDef.displayName} §f사용!"))
        SidebarService.updateNow(p)
    }
}
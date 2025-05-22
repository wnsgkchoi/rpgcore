package org.flash.rpgcore.util

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.classs.PlayerClass
import org.flash.rpgcore.equipment.EquipSlot
import org.flash.rpgcore.equipment.EquipmentDefs
import org.flash.rpgcore.equipment.ItemKeys

object WeaponRules {

    private val ACTION_MATERIALS_BY_CLASS: Map<PlayerClass, Set<Material>> = mapOf(
        PlayerClass.BERSERKER to setOf(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
        ),
        PlayerClass.TANK to setOf(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
        ),
        PlayerClass.ELEMENTIST to setOf(
            Material.STICK, Material.BLAZE_ROD, Material.BREEZE_ROD
        ),
        PlayerClass.HITMAN to setOf(Material.CROSSBOW),
        PlayerClass.SNIPER to setOf(Material.BOW)
        // NOVICE는 아래 로직에서 직접 처리
    )

    /**
     * RPG 아이템을 장비 GUI의 '무기 슬롯'에 장착할 수 있는지 여부를 판단합니다.
     * NOVICE는 어떤 WEAPON도 장착할 수 없습니다.
     */
    fun isEquippable(cls: PlayerClass, rpgWeaponCandidate: ItemStack?): Boolean {
        println("--- isEquippable called ---") // 디버깅 로그
        println("Player class: $cls") // 디버깅 로그
        println("Weapon candidate: ${rpgWeaponCandidate?.type}, equipId: ${rpgWeaponCandidate?.itemMeta?.persistentDataContainer?.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING)}") // 디버깅 로그

        if (cls == PlayerClass.NOVICE) {
            println("Result: false (NOVICE cannot equip)") // 디버깅 로그
            return false
        }

        if (rpgWeaponCandidate == null || rpgWeaponCandidate.type == Material.AIR) {
            println("Result: false (Weapon candidate is null or AIR)") // 디버깅 로그
            return false
        }

        val itemMeta = rpgWeaponCandidate.itemMeta
        if (itemMeta == null) {
            println("Result: false (ItemMeta is null)") // 디버깅 로그
            return false
        }
        val pdc = itemMeta.persistentDataContainer
        val equipId = pdc.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING)
        if (equipId == null) {
            println("Result: false (equipId is null)") // 디버깅 로그
            return false
        }
        val def = EquipmentDefs.get(equipId)
        if (def == null) {
            println("Result: false (EquipmentDef not found for $equipId)") // 디버깅 로그
            return false
        }
        println("EquipmentDef found: ${def.id}, slot: ${def.slot}, validClasses: ${def.validClasses}") // 디버깅 로그

        if (def.slot != EquipSlot.WEAPON) {
            println("Result: false (Not a WEAPON slot item)") // 디버깅 로그
            return false
        }

        val result = def.validClasses.isNotEmpty() && cls in def.validClasses
        println("Final check: validClasses.isNotEmpty()=${def.validClasses.isNotEmpty()}, cls in validClasses=${cls in def.validClasses}, Result: $result") // 디버깅 로그
        return result
    }

    /**
     * 현재 손에 든 아이템과 장비된 RPG 무기를 바탕으로 일반 공격/스킬 시전 등의 액션을 수행할 수 있는지 판단합니다.
     * NOVICE는 장착된 무기가 없고(null), 손에 무엇을 들든(또는 맨손이든) 액션이 가능합니다.
     */
    fun canPerformAction(
        cls: PlayerClass,
        itemInHand: ItemStack?,
        equippedRpgWeapon: ItemStack? // 장비 GUI의 무기 슬롯에 장착된 아이템
    ): Boolean {
        if (cls == PlayerClass.NOVICE) {
            // NOVICE는 장착된 무기가 없어야 하고 (있다면 비정상 상태 또는 규칙 위반)
            // 손에는 무엇을 들고 있든 (또는 맨손이든) 액션 가능
            return equippedRpgWeapon == null || equippedRpgWeapon.type == Material.AIR
        }

        // NOVICE가 아닌 다른 클래스의 경우:
        // 1. 장비창에 RPG 무기가 제대로 장착되어 있는지 확인
        if (!isEquippable(cls, equippedRpgWeapon)) {
            // isEquippable은 cls가 NOVICE가 아닐 때만 호출되도록 위에서 분기했으므로,
            // 여기서 false가 나오면 해당 클래스는 그 무기를 장착할 수 없다는 의미.
            return false
        }
        // 여기서부터 equippedRpgWeapon은 cls에 대해 장착 가능한 유효한 RPG 무기임.

        // 2. 손에 든 아이템(itemInHand)의 Material이 현재 클래스에 적합한지 확인
        if (itemInHand == null || itemInHand.type == Material.AIR) {
            // Novice 외 다른 직업은 맨손 액션 불가 (지정된 Material을 들어야 함)
            return false
        }

        val requiredMaterials = ACTION_MATERIALS_BY_CLASS[cls]

        return requiredMaterials != null && itemInHand.type in requiredMaterials
    }
}
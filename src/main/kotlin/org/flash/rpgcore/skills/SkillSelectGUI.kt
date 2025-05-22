// File: SkillSelectGUI.kt
package org.flash.rpgcore.skills

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.classs.PlayerClass
import java.util.*

object SkillSelectGUI {
    const val TITLE = "§6스킬 선택 GUI"
    private const val SIZE = 54

    // 어느 슬롯(1~3)을 교체하려 했는지
    private val sessionSlot = mutableMapOf<UUID, Int>()
    // open() 시점에 뿌린 available 리스트를 그대로 꺼내기 위함
    private val sessionAvailable = mutableMapOf<UUID, List<SkillDefinition>>()

    fun open(player: Player, slotIndex: Int) {
        val uuid   = player.uniqueId
        val skills = SkillManager.getSkills(uuid)
        val cls    = ClassManager.get(uuid)

        if (ClassManager.get(player.uniqueId) == PlayerClass.NOVICE) {
            player.sendMessage("§cNOVICE는 스킬을 장착하거나 선택할 수 없습니다.") // 또는 다른 적절한 메시지
            player.closeInventory() // GUI를 바로 닫거나, 빈 화면 또는 안내 화면 표시
            return
        }

        // 1) 이미 장착된 ID 수집
        val equippedIds = listOfNotNull(
            skills.slot1?.first,
            skills.slot2?.first,
            skills.slot3?.first
        ).toSet()

        // 2) learned(키) + 클래스 허용 + 미장착 필터링
        val available = skills.learned.keys
            .mapNotNull { SkillDefs.get(it) }
            .filter { def -> cls in def.allowedClasses }
            .filter { def -> def.id !in equippedIds }

        // 3) 캐시에 저장
        sessionSlot[uuid]      = slotIndex
        sessionAvailable[uuid] = available

        // 4) 인벤토리에 뿌리기
        val inv: Inventory = Bukkit.createInventory(null, SIZE, Component.text(TITLE))
        available.forEachIndexed { idx, def ->
            if (idx >= SIZE) return@forEachIndexed
            inv.setItem(idx, makeIcon(player, def))
        }
        player.openInventory(inv)
    }

    /** 클릭 후 “어느 슬롯을 바꾸려 했는지” 반환 */
    fun popSessionSlot(uuid: UUID): Int? =
        sessionSlot.remove(uuid)

    /** 클릭 후 “그때 뿌린 available 리스트”를 그대로 꺼냄 */
    fun popSessionAvailable(uuid: UUID): List<SkillDefinition>? =
        sessionAvailable.remove(uuid)

    private fun makeIcon(player: Player, def: SkillDefinition): ItemStack {
        val uuid   = player.uniqueId
        val skills = SkillManager.getSkills(uuid)

        // learned 맵에서 레벨 꺼내기(없으면 기본 1)
        val curLevel = skills.learned[def.id] ?: 1

        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta!!
        meta.displayName(Component.text(def.displayName, NamedTextColor.GOLD))

        val lore = mutableListOf<Component>()
        def.description.forEach { lore += Component.text(it, NamedTextColor.WHITE) }
        lore += Component.text("현재 레벨: $curLevel", NamedTextColor.AQUA)
        lore += Component.text("MP 필요: ${def.getMpCost(curLevel)}", NamedTextColor.LIGHT_PURPLE)
        lore += Component.text(
            "계수: 물리 +${"%.2f".format(def.getAtkCoef(curLevel))}, 마법 +${"%.2f".format(def.getMagCoef(curLevel))}",
            NamedTextColor.LIGHT_PURPLE
        )
        lore += Component.text("쿨타임: ${"%.1f".format(def.getCooldown(curLevel))}s", NamedTextColor.LIGHT_PURPLE)

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }
}

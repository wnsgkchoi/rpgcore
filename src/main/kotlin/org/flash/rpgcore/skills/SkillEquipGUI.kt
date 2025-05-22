// File: SkillEquipGUI.kt
package org.flash.rpgcore.skills

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.classs.PlayerClass

object SkillEquipGUI {
    const val TITLE = "§6스킬 장착 GUI"

    // info / enhance / change 아이콘 재사용
    private val ENH_ICON   = ItemStack(Material.ANVIL)
    private val CHANGE_ICON= ItemStack(Material.PAPER)

    fun open(player: Player) {
        val inv: Inventory = player.server.createInventory(null, 9, Component.text(TITLE))
        val uuid = player.uniqueId
        val skills = SkillManager.getSkills(uuid)

        if (ClassManager.get(player.uniqueId) == PlayerClass.NOVICE) {
            player.sendMessage("§cNOVICE는 스킬을 장착하거나 선택할 수 없습니다.") // 또는 다른 적절한 메시지
            player.closeInventory() // GUI를 바로 닫거나, 빈 화면 또는 안내 화면 표시
            return
        }

        for (slot in 1..3) {
            val baseIdx = (slot - 1) * 3
            val entry   = skills.get(slot)
            // 1) 정보 슬롯
            inv.setItem(baseIdx, buildInfo(entry))
            // 2) 강화 버튼
            inv.setItem(baseIdx + 1, buildEnhanceButton(entry))
            // 3) 교체 버튼
            inv.setItem(baseIdx + 2, buildChangeButton(slot))
        }

        player.openInventory(inv)
    }

    private fun buildInfo(entry: Pair<String, Int>?): ItemStack {
        if (entry == null) return emptySlot()
        val (skillId, lvl) = entry
        val def   = SkillDefs.get(skillId)!!
        val item  = ItemStack(Material.BOOK)
        val meta  = item.itemMeta!!
        meta.displayName(Component.text(def.displayName, NamedTextColor.GOLD))

        val lore = mutableListOf<Component>()
        // 1) 설명
        def.description.forEach {
            lore += Component.text(it, NamedTextColor.WHITE)
        }
        // 2) 현재 레벨
        lore += Component.text("현재 레벨: $lvl", NamedTextColor.AQUA)
        // 3) 현재 MP 비용 / 물리·마법 계수 / 쿨타임
        val useMp     = def.getMpCost(lvl)
        val physCoef  = def.getAtkCoef(lvl)
        val magCoef   = def.getMagCoef(lvl)
        val cooldown  = def.getCooldown(lvl)
        lore += Component.text("MP 필요: $useMp", NamedTextColor.LIGHT_PURPLE)
        lore += Component.text("계수: 물리+$physCoef, 마법+$magCoef", NamedTextColor.LIGHT_PURPLE)
        lore += Component.text("쿨타임: ${cooldown}s", NamedTextColor.LIGHT_PURPLE)

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun buildEnhanceButton(entry: Pair<String, Int>?): ItemStack {
        val item = ENH_ICON.clone()
        val meta = item.itemMeta!!
        meta.displayName(Component.text("§a스킬 강화", NamedTextColor.GREEN))

        val lore = mutableListOf<Component>()
        if (entry == null) {
            lore += Component.text("장착된 스킬이 없습니다", NamedTextColor.RED)
        } else {
            val (skillId, lvl) = entry
            val def = SkillDefs.get(skillId)!!
            lore += Component.text("Max 레벨: ${def.maxLevel}", NamedTextColor.YELLOW)

            if (lvl >= def.maxLevel) {
                lore += Component.text("최대 레벨입니다", NamedTextColor.RED)
            } else {
                val next      = lvl + 1
                val xpCost    = def.getXpCost(next)
                val sucRate   = def.getSucRate(next)
                val physNext  = def.getAtkCoef(next)
                val magNext   = def.getMagCoef(next)
                val mpNext    = def.getMpCost(next)
                val cdNext    = def.getCooldown(next)

                lore += Component.text("필요 XP: $xpCost", NamedTextColor.GRAY)
                lore += Component.text("성공 확률: ${"%.1f".format(sucRate)}%", NamedTextColor.GRAY)
                lore += Component.text("다음 계수: 물리+$physNext, 마법+$magNext", NamedTextColor.YELLOW)
                lore += Component.text("다음 MP: $mpNext", NamedTextColor.LIGHT_PURPLE)
                lore += Component.text("다음 쿨타임: ${cdNext}s", NamedTextColor.LIGHT_PURPLE)
            }
        }

        meta.lore(lore)
        item.itemMeta = meta
        return item
    }

    private fun buildChangeButton(slot: Int): ItemStack {
        val item = CHANGE_ICON.clone()
        val meta = item.itemMeta!!
        meta.displayName(Component.text("§e스킬 교체", NamedTextColor.YELLOW))
        meta.lore(listOf(Component.text("클릭하여 스킬을 교체합니다", NamedTextColor.GRAY)))
        item.itemMeta = meta
        return item
    }

    private fun emptySlot(): ItemStack =
        ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1).apply {
            itemMeta = itemMeta!!.apply {
                displayName(Component.text(" ", NamedTextColor.GRAY))
                addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
            }
        }
}

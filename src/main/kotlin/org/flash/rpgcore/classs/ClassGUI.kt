package org.flash.rpgcore.classs

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

object ClassGUI {
    private val LEGACY = LegacyComponentSerializer.legacySection()

    fun open(p: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 4*9, Component.text("클래스 전직 (xp 300000)"))
        fun icon(mat: Material, name: String, lore: List<String>) =
            ItemStack(mat).apply {
                val m = itemMeta
                m.displayName(LEGACY.deserialize(name))
                m.lore(lore.map { LEGACY.deserialize(it) })
                itemMeta = m
            }
        
        inv.setItem(0, icon(Material.PAPER, "전사", listOf("근거리 계열 직업군")))
        inv.setItem(1, icon(Material.IRON_AXE, "§c광전사", listOf("§7사용 무기: 도끼", "§7패시브: 체력 손실 비례 추가 피해", "§7낮은 체력을 유지하며 싸우는 것이 중요한 근접공격 클래스")))
        inv.setItem(2, icon(Material.IRON_SWORD, "§6탱커", listOf("§7사용 무기: 검", "§7패시브: 받는 피해량 비례 반사 데미지, 피격 데미지 감소", "§7공격력이 약하지만 잘 죽지 않는 근접공격 클래스")))

        inv.setItem(9, icon(Material.PAPER, "원거리 딜러", listOf("원거리 계열 직업군")))

        inv.setItem(18, icon(Material.PAPER, "마법사", listOf("스킬 기반 직업군", "피격 데미지의 일부를 MP로 대신함")))
        inv.setItem(19, icon(Material.DIAMOND_SHOVEL, "§d원소술사", listOf("§7사용 무기: 삽", "§7세 가지 스택(버닝, 패럴라이징, 프리징)을 잘 운용해야 하는 클래스")))

        inv.setItem(31, icon(Material.BARRIER, "전직 취소", listOf("전직을 취소하고 초보자로 돌아갑니다. 현재 장착 중인 무기가 사라집니다.")))
        p.openInventory(inv)
    }
}
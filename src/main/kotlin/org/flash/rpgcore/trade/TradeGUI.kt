// src/main/kotlin/org/flash/rpgcore/trade/TradeGUI.kt
package org.flash.rpgcore.trade

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

object TradeGUI {
    private val TITLE = Component.text("§6§l거래 중")

    private const val SIZE = 54
    private val xpIcon    = ItemStack(Material.EXPERIENCE_BOTTLE)
    private val acceptIcon= ItemStack(Material.GREEN_WOOL)

    /**
     * 세션에 맞는 빈 GUI 생성
     */
    fun create(session: TradeSession): Inventory {
        val inv = Bukkit.createInventory(null, SIZE, TITLE)

        // 1) 빈 칸은 가시성 용 무늬 유리
        val filler = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        for (i in 0 until SIZE) inv.setItem(i, filler)

        // 2) XP 버튼
        inv.setItem(4, xpIcon.clone().applyMeta("XP 입금"))
        inv.setItem(49, xpIcon.clone().applyMeta("XP 입금"))

        // 3) 수락 버튼
        inv.setItem(22, acceptIcon.clone().applyMeta("거래 수락"))
        inv.setItem(31, acceptIcon.clone().applyMeta("거래 수락"))

        // 4) (아이템 제안은 빈 칸을 클릭/드래그로 직접 넣도록 두기)

        return inv
    }

    /** 간단한 ItemStack + displayName 유틸 */
    private fun ItemStack.applyMeta(name: String): ItemStack {
        itemMeta = (itemMeta as ItemMeta).also {
            it.setDisplayName(name)
            it.isUnbreakable = true
            it.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        }
        return this
    }
}

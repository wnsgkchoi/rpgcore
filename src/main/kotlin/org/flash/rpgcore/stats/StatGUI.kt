package org.flash.rpgcore.stats

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.util.VirtualHp
import org.flash.rpgcore.util.VirtualMp
import java.util.*

class StatGUI(private val owner: UUID) {

    companion object {
        private val LEGACY = LegacyComponentSerializer.legacySection()
        fun open(p: Player) = StatGUI(p.uniqueId).show(p)
    }

    private fun idx(r: Int, c: Int) = r * 9 + c     // 6 rows → size 54

    fun show(p: Player) {
        val inv: Inventory = Bukkit.createInventory(null, 54, Component.text("능력치 관리"))
        val stats = StatManager.load(owner)

        StatType.values().forEach { type ->
            val row = type.row
            inv.setItem(idx(row, 4), display(type.display, stats.get(type)))
            inv.setItem(idx(row, 8), plusButton(type))
        }

        p.openInventory(inv)
    }

    private fun display(name: String, value: Int): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta ?: return item
        meta.displayName(LEGACY.deserialize("§e$name: §f$value"))
        item.itemMeta = meta
        return item
    }

    private fun plusButton(type: StatType): ItemStack {
        val stats = StatManager.load(owner)

        // 기존 증가량 계산
        val (base, incPer) = when (type) {
            StatType.HEALTH  -> 20 to 2
            StatType.MP      -> 50 to 5
            else             -> 5  to 1
        }
        val already = (stats.get(type) - base) / incPer
        val cost    = (already + 1) * 50

        val item = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        val meta = item.itemMeta ?: return item

        // 이름
        meta.displayName(LEGACY.deserialize("§a클릭 → ${type.display} 증가"))

        // ✂️ 여기만 바뀌었습니다: Component 기반 lore setter 사용
        meta.lore(listOf(
            LEGACY.deserialize("§7필요 XP: §f$cost")
        ))

        // PDC 에 StatType 저장 (클릭 시 어떤 stat 인지 식별)
        meta.persistentDataContainer.set(
            NamespacedKey(RPGCore.INSTANCE, "stat_type"),
            PersistentDataType.STRING,
            type.name
        )
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        item.itemMeta = meta
        return item
    }
}

object NamespacedKeys {
    val STAT_TYPE: NamespacedKey = NamespacedKey(RPGCore.INSTANCE, "stat_type")
}

class StatListener : Listener {

    /* 플레이어 접속 시 캐시 & 사이드바 준비 */
    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        // 1) 캐시 초기화 & yml 재로드
        StatManager.clear(p.uniqueId)
        StatManager.load(p.uniqueId)
        StatManager.recalcFor(p)
        StatManager.updatePlayerAttributes(p)

        // 2) 가상 HP/MP 최대치로 채우기
        VirtualHp.fillMax(p)
        VirtualMp.fillMax(p)

        SidebarService.updateNow(p)
    }

    /* 스탯 GUI 버튼 클릭 */
    @EventHandler(ignoreCancelled = true)
    fun onClick(e: InventoryClickEvent) {
        val titleComponent: Component = e.view.title()
        val title: String = PlainTextComponentSerializer.plainText().serialize(titleComponent)

        if (title == "장비 관리") return
        if (title != "능력치 관리") return

        e.isCancelled = true

        val meta = e.currentItem?.itemMeta ?: return
        val typeName = meta.persistentDataContainer
            .get(NamespacedKeys.STAT_TYPE, PersistentDataType.STRING) ?: return
        val type = StatType.valueOf(typeName)

        val p = e.whoClicked as Player
        if (StatManager.tryIncrease(p, type)) {
            StatGUI.open(p)                                       // GUI 새로고침
        }
    }
}
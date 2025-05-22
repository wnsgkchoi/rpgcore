package org.flash.rpgcore.listener

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/**
 * • 특정 무기/방어구/폭죽/철퇴 등의 사용을 막고
 * • 인벤토리에 들어오는 순간 인챈트를 제거합니다.
 */
class ForbiddenItemListener : Listener {

    // 사용을 완전히 금지할 무기·방어구·아이템
    private val enchantForbiddenTypes = setOf(
        // 무기
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
        Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
        Material.DIAMOND_AXE, Material.NETHERITE_AXE,
        Material.BOW, Material.CROSSBOW, Material.TRIDENT,
        // 철퇴 (플레이어가 쥘 수 있는 몹 낚싯대류가 없으니 예시로 철괴)
        Material.LODESTONE, // <-- 실제 커스텀 아이템이라면 여기에 추가
        // 방어구 (인챈트 방지 목적)
        Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
        Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
        Material.IRON_HELMET, Material.IRON_CHESTPLATE,
        Material.IRON_LEGGINGS, Material.IRON_BOOTS,
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
        Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
        Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
        Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
    )

    private val useForbiddenTypes = setOf(
        // 폭죽
        Material.FIREWORK_ROCKET, Material.FIREWORK_STAR
    )

    // 1) 플레이어가 인벤토리에 아이템을 집을 때 → 인챈트 제거
    @EventHandler
    fun onPickup(e: PlayerPickupItemEvent) {
        val stack = e.item.itemStack          // ← 엔티티가 아니라 실질적 ItemStack!
        if (stack.type in enchantForbiddenTypes) {
            stripEnchants(stack)
        }
    }

    // 2) 인벤토리 클릭 시에도 → 인챈트 제거
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        // 클릭된 슬롯에 있던 아이템
        e.currentItem?.let { item ->
            if (item.type in enchantForbiddenTypes) {
                stripEnchants(item)
            }
        }
        // 마우스 커서 위에 있던 아이템
        e.cursor?.let { cursor ->
            if (cursor.type in enchantForbiddenTypes) {
                stripEnchants(cursor)
            }
        }
    }

    // 3) 우클릭·좌클릭 사용 자체를 막기
    @EventHandler
    fun onUse(e: PlayerInteractEvent) {
        val hand = e.hand
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) return

        e.player.inventory.itemInMainHand.let { stack ->
            if (stack.type in useForbiddenTypes) {
                e.isCancelled = true
                e.player.sendActionBar("§c이 아이템은 사용할 수 없습니다.")
            }
        }
    }

    // 아이템에서 모든 인챈트를 떼고, 내구도도 풀리면 유지
    private fun stripEnchants(item: ItemStack) {
        if (item.enchantments.isNotEmpty()) {
            item.enchantments.keys.forEach { ench -> item.removeEnchantment(ench) }
        }
        (item.itemMeta as? Damageable)?.let { dmgMeta ->
            dmgMeta.damage = 0
            item.itemMeta = dmgMeta
        }
    }
}

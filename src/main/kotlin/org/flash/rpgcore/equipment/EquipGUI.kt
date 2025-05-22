package org.flash.rpgcore.equipment

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.equipment.EquipSlot
import org.flash.rpgcore.equipment.EquipSlot.*
import java.util.*

object EquipGUI {
    private const val SIZE = 54
    val TITLE: Component = LegacyComponentSerializer.legacySection().deserialize("§6§l[장비 창]")
    // LEGACY_SERIALIZER 선언
    private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()
    private val plugin = RPGCore.INSTANCE

    // 12 부위 → (row, col) 인벤 슬롯 매핑
    private val PART_SLOT = mapOf(
        EquipSlot.HELMET    to Pair(0,1),
        EquipSlot.RING      to Pair(0,4),
        EquipSlot.NECKLACE  to Pair(0,7),
        EquipSlot.CHEST     to Pair(1,1),
        EquipSlot.BRACELET  to Pair(1,4),
        EquipSlot.CAPE      to Pair(1,7),
        EquipSlot.LEGS      to Pair(2,1),
        EquipSlot.GLOVES    to Pair(2,4),
        EquipSlot.EARRING   to Pair(2,7),
        EquipSlot.BOOTS     to Pair(3,1),
        EquipSlot.BELT      to Pair(3,4),
        EquipSlot.WEAPON    to Pair(3,7)
    )

    private fun idx(r: Int, c: Int) = r*9 + c
    val SLOT_IDX = PART_SLOT.mapValues { (_, rc) -> idx(rc.first, rc.second) }
    val ENH_IDX  = PART_SLOT.mapValues { (_, rc) -> idx(rc.first, rc.second + 1) }
    val CRAFT_IDX = 49

    fun open(player: Player) {
        // 경고 발생 지점(EquipGUI.kt:82)은 이 함수 내부, 특히 displayName 설정 부분일 가능성이 높음
        // player.openInventory(inv) 호출이 82번째 줄 근처라면, 그 이전에 생성된 inv의 아이템들 확인 필요.

        val inv: Inventory = Bukkit.createInventory(null, SIZE, TITLE) // TITLE은 이미 Component

        // 회색 유리 라벨 - 수정된 부분
        fun label(text: String) = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta!!.apply {
                displayName(LEGACY_SERIALIZER.deserialize(text)) // Component.text(text) 대신 deserialize 사용
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }

        // Anvil 아이콘 (강화 버튼) - 수정된 부분
        val anvil = ItemStack(Material.ANVIL).apply {
            itemMeta = itemMeta!!.apply {
                displayName(LEGACY_SERIALIZER.deserialize("§e우클릭: 강화")) // Component.text() 대신 deserialize 사용
                // lore도 필요하다면 같은 방식으로 처리
                addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            }
        }

        // ① 라벨 배치
        SLOT_IDX.forEach { (partKey, slot) ->
            // partKey.name.lowercase().uppercase() 는 결국 partKey.name 과 동일.
            // Enum 이름을 그대로 사용하는 것보다 더 명확한 표시 이름이 있다면 그것을 사용.
            // 여기서는 기존 로직 유지.
            inv.setItem(slot - 1, label("§f[${partKey.name.uppercase()}]"))
        }

        // ② 장착 아이템 + 강화 버튼
        SLOT_IDX.forEach { (part, slot) ->
            // EquipStore.loadItem은 RPG 아이템(이미 PDC와 적절한 displayName Component가 설정된)을 반환해야 함.
            // 만약 여기서 반환된 아이템의 displayName이 레거시 문자열이라면 문제가 될 수 있으나,
            // ItemFactory에서 Component로 생성하므로 이 부분은 괜찮을 가능성이 높음.
            val item = EquipStore.loadItem(player.uniqueId, part.name.lowercase())
            inv.setItem(slot, item ?: ItemStack(Material.AIR)) // item이 null이면 AIR

            inv.setItem(ENH_IDX[part]!!, anvil.clone()) // anvil 아이콘 복제해서 사용
        }

        // ③ 제작 버튼 - 수정된 부분
        inv.setItem(CRAFT_IDX,
            ItemStack(Material.CRAFTING_TABLE).apply {
                itemMeta = itemMeta!!.apply {
                    displayName(LEGACY_SERIALIZER.deserialize("§a장비 제작")) // Component.text() 대신 deserialize 사용
                    addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                }
            }
        )
        // 이 inv 객체가 생성된 후, player.openInventory(inv) 호출이 스택 트레이스의 82번째 줄 근처일 것입니다.
        player.openInventory(inv)
    }
}

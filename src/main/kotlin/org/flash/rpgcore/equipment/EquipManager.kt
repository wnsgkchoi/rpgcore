// File: EquipManager.kt
package org.flash.rpgcore.equipment

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.stats.StatManager
import java.util.UUID

object EquipManager {

    data class Slots(
        var weapon:   ItemStack? = null,
        var helmet:   ItemStack? = null,
        var chest:    ItemStack? = null,
        var legs:     ItemStack? = null,
        var boots:    ItemStack? = null,
        var ring:     ItemStack? = null,
        var necklace: ItemStack? = null,
        var bracelet: ItemStack? = null,
        var cape:     ItemStack? = null,
        var gloves:   ItemStack? = null,
        var earring:  ItemStack? = null,
        var belt:     ItemStack? = null
    ) {
        fun toMap(): Map<EquipSlot, ItemStack?> = mapOf(
            EquipSlot.WEAPON   to weapon,
            EquipSlot.HELMET   to helmet,
            // ... (다른 슬롯들)
            EquipSlot.BELT     to belt
        )

        fun get(part: EquipSlot): ItemStack? = when (part) {
            EquipSlot.WEAPON   -> weapon
            EquipSlot.HELMET   -> helmet
            EquipSlot.CHEST    -> chest
            EquipSlot.LEGS     -> legs
            EquipSlot.BOOTS    -> boots
            EquipSlot.RING     -> ring
            EquipSlot.NECKLACE -> necklace
            EquipSlot.BRACELET -> bracelet
            EquipSlot.CAPE     -> cape
            EquipSlot.GLOVES   -> gloves
            EquipSlot.EARRING  -> earring
            EquipSlot.BELT     -> belt
        }

        // 'set' 메소드 추가
        fun set(part: EquipSlot, item: ItemStack?) {
            when (part) {
                EquipSlot.WEAPON   -> weapon   = item
                EquipSlot.HELMET   -> helmet   = item
                EquipSlot.CHEST    -> chest    = item
                EquipSlot.LEGS     -> legs     = item
                EquipSlot.BOOTS    -> boots    = item
                EquipSlot.RING     -> ring     = item
                EquipSlot.NECKLACE -> necklace = item
                EquipSlot.BRACELET -> bracelet = item
                EquipSlot.CAPE     -> cape     = item
                EquipSlot.GLOVES   -> gloves   = item
                EquipSlot.EARRING  -> earring  = item
                EquipSlot.BELT     -> belt     = item
            }
        }
    }

    /* 레시피 학습 여부 PDC 키 */
    private val LEARNED_KEY = NamespacedKey(RPGCore.INSTANCE, "learn_recipe")

    fun hasLearned(p: Player, recipeId: String): Boolean =
        p.persistentDataContainer
            .getOrDefault(LEARNED_KEY, PersistentDataType.STRING, "")
            .split(';')
            .contains(recipeId)

    fun learn(p: Player, recipeId: String) {
        val cur = p.persistentDataContainer
            .getOrDefault(LEARNED_KEY, PersistentDataType.STRING, "")
        val joined = (cur.split(';') + recipeId)
            .filter { it.isNotEmpty() }
            .joinToString(";")
        p.persistentDataContainer.set(LEARNED_KEY, PersistentDataType.STRING, joined)
    }

    /* 슬롯 캐시 + 최초 로드 시 EquipStore에서 raw 로드 & PDC 누락 시 재생성 */
    private val slotMap = hashMapOf<UUID, Slots>()

    fun getSlots(uuid: UUID): Slots =
        slotMap.computeIfAbsent(uuid) {
            val s = Slots()

            EquipSlot.values().forEach { part ->
                val keyName = part.name.lowercase()
                val rawItem = EquipStore.loadItem(uuid, keyName)

                // AIR가 아니면, PDC에 스탯 키가 있는지 검사
                val fixed: ItemStack? = rawItem
                    ?.takeIf { it.type != Material.AIR }
                    ?.let { item ->
                        val pdc = item.itemMeta?.persistentDataContainer
                        val hasStats = pdc
                            ?.has(ItemKeys.HP, PersistentDataType.INTEGER) == true

                        if (!hasStats) {
                            // 구버전 데이터: equip_id / enh_level 만 남아있음 → 재생성
                            val id  = pdc!!.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING)!!
                            val lvl = pdc.getOrDefault(
                                ItemKeys.ENH_LEVEL, PersistentDataType.INTEGER, 0
                            )
                            val def       = EquipmentDefs.get(id)!!
                            val recreated = ItemFactory.create(def, lvl)
                            // 재저장하여 다음 로드 때는 정상 PDC가 남아 있게 함
                            EquipStore.saveItem(uuid, keyName, recreated)
                            recreated
                        } else {
                            // 이미 정상 PDC가 들어있는 아이템
                            item
                        }
                    }

                // 슬롯에 장착
                when (part) {
                    EquipSlot.WEAPON   -> s.weapon   = fixed
                    EquipSlot.HELMET   -> s.helmet   = fixed
                    EquipSlot.CHEST    -> s.chest    = fixed
                    EquipSlot.LEGS     -> s.legs     = fixed
                    EquipSlot.BOOTS    -> s.boots    = fixed
                    EquipSlot.RING     -> s.ring     = fixed
                    EquipSlot.NECKLACE -> s.necklace = fixed
                    EquipSlot.BRACELET -> s.bracelet = fixed
                    EquipSlot.CAPE     -> s.cape     = fixed
                    EquipSlot.GLOVES   -> s.gloves   = fixed
                    EquipSlot.EARRING  -> s.earring  = fixed
                    EquipSlot.BELT     -> s.belt     = fixed
                }
            }

            s
        }

    fun refreshAllPlayers() {
        Bukkit.getOnlinePlayers().forEach { p ->
            val uuid = p.uniqueId
            val slots = getSlots(uuid)
            EquipSlot.values().forEach { part ->
                val key = part.name.lowercase()
                val stored = EquipStore.loadItem(uuid, key) ?: return@forEach
                val pdc    = stored.itemMeta?.persistentDataContainer ?: return@forEach
                val id     = pdc.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING) ?: return@forEach
                val lvl    = pdc.getOrDefault(ItemKeys.ENH_LEVEL, PersistentDataType.INTEGER, 0)
                val def    = EquipmentDefs.get(id) ?: return@forEach
                val neo    = ItemFactory.create(def, lvl)
                // 저장 & 캐시 반영
                EquipStore.saveItem(uuid, key, neo)
                when (part) {
                    EquipSlot.WEAPON   -> slots.weapon   = neo
                    EquipSlot.HELMET   -> slots.helmet   = neo
                    EquipSlot.CHEST    -> slots.chest    = neo
                    EquipSlot.LEGS     -> slots.legs     = neo
                    EquipSlot.BOOTS    -> slots.boots    = neo
                    EquipSlot.RING     -> slots.ring     = neo
                    EquipSlot.NECKLACE -> slots.necklace = neo
                    EquipSlot.BRACELET -> slots.bracelet = neo
                    EquipSlot.CAPE     -> slots.cape     = neo
                    EquipSlot.GLOVES   -> slots.gloves   = neo
                    EquipSlot.EARRING  -> slots.earring  = neo
                    EquipSlot.BELT     -> slots.belt     = neo
                }
            }
            // 재계산 & GUI 갱신
            StatManager.recalcFor(p)
            if (p.openInventory?.title() == EquipGUI.TITLE) {
                EquipGUI.open(p)
            }
        }
    }
}

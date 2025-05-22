package org.flash.rpgcore.equipment

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.Listener
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.Material
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.stats.StatManager
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.util.WeaponRules
import java.util.Random
import java.util.UUID

class EquipListener : Listener {
    private val store = EquipStore
    // LEGACY_SERIALIZER 선언
    private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()

    @EventHandler(ignoreCancelled = true)
    fun onClick(e: InventoryClickEvent) {

        // 1) 장비창이 아니면 무시
        // e.view.title()은 Component를 반환. EquipGUI.TITLE도 Component.
        // Component 비교는 contentEquals 또는 직접 equals 사용.
        if (e.view.title() != EquipGUI.TITLE) { // Component 객체 비교는 이렇게 해도 될 수 있으나, content 비교가 더 안전
            // println("[EquipListener] Not EquipGUI. GUI Title: '${PlainTextComponentSerializer.plainText().serialize(e.view.title())}' Expected: '${PlainTextComponentSerializer.plainText().serialize(EquipGUI.TITLE)}'")
            return
        }
        val player = e.whoClicked as? Player ?: return

        // 2) rawSlot 으로 “상단 vs 하단” 분기
        val topInv = e.view.topInventory
        if (e.rawSlot >= topInv.size) { // topInv.size (54)
            return // 일단 하단 인벤토리 클릭은 여기서 처리하지 않음
        }

        // 3) 이제부터 상단 클릭 → 기본 이동 동작 모두 차단 (대부분의 커스텀 GUI의 기본 처리)
        e.isCancelled = true

        val uuid = player.uniqueId

        // 4) 인덱스 ↔ 파츠 매핑 준비 (EquipGUI에서 가져옴)
        val partMap = EquipGUI.SLOT_IDX.entries.associate { it.value to it.key } // slot index -> EquipSlot
        val enhMap  = EquipGUI.ENH_IDX.entries.associate  { it.value to it.key } // enhance button index -> EquipSlot

        // 5) Shift-클릭으로 언장착 지원 (handleEquipUnequip 내부에서 cursor와 currentItem 상태로 판단 가능)
        // 또는 여기서 명시적으로 Shift-클릭을 감지하여 handleEquipUnequip에 플래그 전달 가능
        // 현재 handleEquipUnequip는 e.click (ClickType)을 직접 보지 않고 cursor와 currentItem으로 판단.
        // Shift 클릭 시 특별한 처리가 필요하면 handleEquipUnequip을 수정하거나 여기서 분기.

        // 6) 일반 클릭(좌/우) 처리
        when {
            // 제작 버튼
            e.rawSlot == EquipGUI.CRAFT_IDX -> {
                // TODO: Recipe GUI 열기
                player.sendMessage("제작 기능은 아직 준비 중입니다.") // 임시 메시지
            }
            // 강화 버튼
            enhMap.containsKey(e.rawSlot) -> {
                val partToEnhance = enhMap[e.rawSlot]!!
                handleEnhance(uuid, partToEnhance, player) // 강화 로직 호출
                // EquipGUI.open(player) // 강화 후 GUI 자동 새로고침 (handleEnhance 내부 또는 여기서)
            }
            // 장착 슬롯 클릭
            partMap.containsKey(e.rawSlot) -> {
                val partToInteract = partMap[e.rawSlot]!!
                handleEquipUnequip(uuid, partToInteract, player, e) // 장착/해제 로직 호출
                // EquipGUI.open(player) // 장착/해제 후 GUI 자동 새로고침 (handleEquipUnequip 내부 또는 여기서)
            }
            // 그 외(장식용 빈슬롯 등)는 아무 동작도 하지 않음
            else -> {
            }
        }
        // 모든 상단 인벤토리 클릭 후 GUI를 한번 새로고침 해주는 것이 상태 동기화에 좋을 수 있음.
        // 단, handleEnhance나 handleEquipUnequip에서 이미 GUI를 다시 연다면 중복 호출이 될 수 있으니 주의.
        // 일단 각 핸들러가 스스로 GUI를 여는 것으로 가정.
        // 만약 각 핸들러가 GUI를 열지 않는다면 여기서 EquipGUI.open(player) 호출.
    }


    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        val uuid  = e.player.uniqueId
        val slots = EquipManager.getSlots(uuid)

        // toMap() 덕분에 각 파츠와 해당 ItemStack? 을 순회하면서 저장 가능
        slots.toMap().forEach { (part, item) ->
            EquipStore.saveItem(uuid, part.name.lowercase(), item)
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) { // onPlayerJoin 리스너 추가 (StatManager.recalcFor 호출용)
        StatManager.recalcFor(e.player)
        // EquipManager.getSlots(e.player.uniqueId) // 접속 시 장비 로드 (EquipManager 내부에서 필요시 자동 로드)
    }

    /**
     * 강화 처리
     */
    private fun handleEnhance(uuid: UUID, part: EquipSlot, player: Player) {
        val slots    = EquipManager.getSlots(uuid)
        val equipped = when (part) {
            EquipSlot.WEAPON   -> slots.weapon
            EquipSlot.HELMET   -> slots.helmet
            EquipSlot.CHEST    -> slots.chest
            EquipSlot.LEGS     -> slots.legs
            EquipSlot.BOOTS    -> slots.boots
            EquipSlot.RING     -> slots.ring
            EquipSlot.NECKLACE -> slots.necklace
            EquipSlot.BRACELET -> slots.bracelet
            EquipSlot.CAPE     -> slots.cape
            EquipSlot.GLOVES   -> slots.gloves
            EquipSlot.EARRING  -> slots.earring
            EquipSlot.BELT     -> slots.belt
        }
        if (equipped == null || equipped.type.isAir) {
            player.sendActionBar("§c장착된 장비가 없습니다")
            return
        }

        val pdc  = equipped.itemMeta!!.persistentDataContainer
        val id   = pdc.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING) ?: return
        val def  = EquipmentDefs.get(id) ?: return
        val lvl  = pdc.getOrDefault(ItemKeys.ENH_LEVEL, PersistentDataType.INTEGER, 0)
        if (lvl >= def.maxEnhance) {
            player.sendActionBar("§c최대 강화 단계입니다")
            return
        }

        // next-level cost & chance
        val xpCost  = def.xpCost[lvl + 1]
        val sucRate = def.successRate[lvl + 1]

        if (player.totalExperience < xpCost) {
            player.sendActionBar("§cXP가 부족합니다: 필요 $xpCost")
            return
        }
        player.giveExp(-xpCost)

        if (Random().nextDouble() < sucRate) {
            val nextItem = ItemFactory.create(def, lvl + 1)

            // ① 메모리 슬롯 업데이트
            when (part) {
                EquipSlot.WEAPON   -> slots.weapon   = nextItem
                EquipSlot.HELMET   -> slots.helmet   = nextItem
                EquipSlot.CHEST    -> slots.chest    = nextItem
                EquipSlot.LEGS     -> slots.legs     = nextItem
                EquipSlot.BOOTS    -> slots.boots    = nextItem
                EquipSlot.RING     -> slots.ring     = nextItem
                EquipSlot.NECKLACE -> slots.necklace = nextItem
                EquipSlot.BRACELET -> slots.bracelet = nextItem
                EquipSlot.CAPE     -> slots.cape     = nextItem
                EquipSlot.GLOVES   -> slots.gloves   = nextItem
                EquipSlot.EARRING  -> slots.earring  = nextItem
                EquipSlot.BELT     -> slots.belt     = nextItem
            }

            // ② 즉시 디스크에 저장 (여기서 빠졌던 부분)
            EquipStore.saveItem(uuid, part.name.lowercase(), nextItem)

            player.sendActionBar("§a강화 성공! 레벨 $lvl → ${lvl + 1}")
        } else {
            player.sendActionBar("§c강화 실패…")
        }

        // ③ 스탯 재계산 & 사이드바 갱신
        StatManager.recalcFor(player)
        SidebarService.updateNow(player)
    }

    /**
     * 장비 장착 / 해제 처리
     */
    private fun handleEquipUnequip(
        uuid: UUID,
        part: EquipSlot,
        player: Player,
        e: InventoryClickEvent
    ) {
        // println("[EquipListener] handleEquipUnequip called for $part. Action: ${e.action}, Click: ${e.click}, Cursor: ${e.cursor?.type}, CurrentItem: ${e.currentItem?.type}")
        val slots  = EquipManager.getSlots(uuid)
        val cursorItem = e.cursor
        val currentSlotItem = e.currentItem

        if ((currentSlotItem != null && currentSlotItem.type != Material.AIR) && (cursorItem.type == Material.AIR)) {
            // println("[EquipListener] Attempting to unequip from $part")
            slots.set(part, null) // 수정: EquipManager.Slots의 set 사용
            EquipStore.saveItem(uuid, part.name.lowercase(), null)

            e.setCursor(currentSlotItem.clone())
            e.currentItem = ItemStack(Material.AIR)

            StatManager.recalcFor(player)
            // SidebarService.updateNow(player)
            player.sendActionBar(LEGACY_SERIALIZER.deserialize("§e${part.name} 해제 완료.")) // 사용
            EquipGUI.open(player)
            return
        }
        else if (cursorItem.type != Material.AIR) {
            if (!WeaponRules.isEquippable(ClassManager.get(player.uniqueId), cursorItem)) { // ClassManager 임포트 필요
                player.sendActionBar(LEGACY_SERIALIZER.deserialize("§c이 아이템은 현재 클래스로 이 슬롯에 장착할 수 없습니다.")) // 사용
                return
            }

            val equipId = cursorItem.itemMeta?.persistentDataContainer?.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING)
            val def = if (equipId != null) EquipmentDefs.get(equipId) else null
            if (def == null || def.slot != part) {
                player.sendActionBar(LEGACY_SERIALIZER.deserialize("§c이 아이템은 ${part.name} 슬롯에 장착할 수 없습니다.")) // 사용
                return
            }

            if (currentSlotItem != null && currentSlotItem.type != Material.AIR) {
                e.currentItem = null
                player.inventory.addItem(currentSlotItem.clone())
                // println("[EquipListener] Swapped out: ${currentSlotItem.type} from $part")
            }

            slots.set(part, cursorItem.clone()) // 수정: EquipManager.Slots의 set 사용
            EquipStore.saveItem(uuid, part.name.lowercase(), cursorItem.clone())

            e.currentItem = cursorItem.clone()
            e.setCursor(ItemStack(Material.AIR))

            StatManager.recalcFor(player)
            SidebarService.updateNow(player)
            player.sendActionBar(LEGACY_SERIALIZER.deserialize("§a${part.name} 장착 완료.")) // 사용
            EquipGUI.open(player)
            return
        }
    }


    /**
     * 이 ItemStack이 해당 부위에 착용 가능한 장비인지 검사
     */
    private fun ItemStack.isValidEquipmentFor(slot: EquipSlot): Boolean {
        if (type.isAir) return false
        val pdc = itemMeta?.persistentDataContainer ?: return false
        val id  = pdc.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING) ?: return false
        val def = EquipmentDefs.get(id) ?: return false
        return def.slot == slot
    }
}

package org.flash.rpgcore.classs

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.flash.rpgcore.equipment.EquipStore
import org.flash.rpgcore.equipment.EquipmentDefs
import org.flash.rpgcore.equipment.ItemFactory
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.util.VirtualHp

class ClassListener : Listener {

    /* 접속 시 사이드바 갱신 */
    @EventHandler
    fun onJoin(e: PlayerJoinEvent) =
        SidebarService.updateNow(e.player)

    /* 전직 GUI 클릭 */
    @EventHandler(ignoreCancelled = true)
    fun onClick(e: InventoryClickEvent) {
        val title = PlainTextComponentSerializer.plainText().serialize(e.view.title())
        if (title != "클래스 전직 (xp 300000)") return         // 전직 GUI 아님

        /* ---------- 기본 차단 ---------- */
        e.isCancelled = true                            // 드래그·이동 불가

        /* 상단 GUI를 클릭한 게 아니라면 무시 */
        if (e.clickedInventory != e.view.topInventory) return

        val p = e.whoClicked as Player
        if (ClassManager.get(p.uniqueId) == PlayerClass.NOVICE) {
            when (e.slot) {
                31 -> {
                    p.sendMessage("§c이미 초보자입니다!")
                    return
                }
                else -> {
                    selClass(p, e.slot)
                }
            }
        } else {
            when (e.slot) {
                31 -> {
                    p.sendMessage("초보자로 돌아갑니다.")
                    ClassManager.set(p.uniqueId, PlayerClass.NOVICE)
                    VirtualHp.fillMax(p)
                    val tmp = ItemStack(Material.AIR, 1)
                    EquipStore.saveItem(p.uniqueId, "weapon", tmp)
                }
                else -> {
                    p.sendMessage("§c재전직하기 위해서는 초보자로 돌아가야 합니다.")
                    return
                }
            }
        }
    }

    private fun selClass (p: Player, slot: Int) {
        /* 레벨 부족? */
        if (p.totalExperience < 300000) {
            p.sendMessage("§c300000xp가 필요합니다.")
            return
        }

        /* 어떤 아이콘을 눌렀는가? */
        val pc = when (slot) {
            1   -> PlayerClass.BERSERKER
            2   -> PlayerClass.TANK
            19  -> PlayerClass.ELEMENTIST
            else -> {return}
        }

        /* 전직 수행 */
        p.giveExp(-300000)
        ClassManager.set(p.uniqueId, pc)
        VirtualHp.fillMax(p)

        // 아이템 생성 및 지급

        val equip = when(pc) {
            PlayerClass.BERSERKER -> "basic_axe"
            PlayerClass.TANK -> "basic_sword"
            PlayerClass.NOVICE -> TODO()
            PlayerClass.HITMAN -> TODO()
            PlayerClass.SNIPER -> TODO()
            PlayerClass.ELEMENTIST -> TODO()
        }

        val def = EquipmentDefs.get(equip)
        val item = def?.let { ItemFactory.create(it, 1) }
        item?.let { p.inventory.addItem(it) }

        p.sendMessage("§a${pc.display} 직업으로 전직 완료!")
        SidebarService.updateNow(p)
        p.closeInventory()
    }
}
package org.flash.rpgcore.trade

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.flash.rpgcore.RPGCore

/**
 * 거래 GUI 상호작용 및 채팅 기반 XP 예치 처리를 담당합니다.
 */
class TradeListener : Listener {

    /**
     * GUI 클릭 처리: XP 버튼, 수락 버튼, 아이템 슬롯
     */
    @EventHandler
    fun onClick(e: InventoryClickEvent) {
        // 거래 중 GUI인지 확인
        if (e.view.title != "§6§l거래 중") return
        val player = e.whoClicked as Player
        val session = TradeManager.getSession(player) ?: return

        // 상단 거래 GUI 클릭인지 확인
        val topInv = e.view.topInventory
        if (e.clickedInventory === topInv) {
            // 거래창 상단 클릭: 버튼만 허용
            e.isCancelled = true
            when (e.rawSlot) {
                // XP 입금 버튼
                4, 49 -> {
                    TradeManager.pendingXpInput.add(player.uniqueId)
                    player.closeInventory()
                    player.sendMessage("§e얼마를 예치할지 채팅으로 입력하세요.")
                }
                // 거래 수락 버튼
                22, 31 -> {
                    session.getOffer(player).accepted = true
                    player.sendMessage("§a거래 수락 완료. 상대방도 수락해야 거래가 성사됩니다.")
                    if (session.bothAccepted()) TradeManager.finalize(session)
                }
                // 그 외 슬롯 클릭 금지
                else -> {}
            }
        } else {
            // 하단 플레이어 인벤토리 클릭: 아이템 이동 허용 (기본 처리)
            e.isCancelled = false
        }
    }

    /**
     * 채팅 입력 처리: XP 예치 후 거래창 재오픈
     */
    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        val player = e.player
        // XP 예치 모드가 아니면 패스
        if (!TradeManager.pendingXpInput.remove(player.uniqueId)) return

        e.isCancelled = true
        val amount = e.message.toIntOrNull()
        if (amount == null || amount < 0) {
            player.sendMessage("§c유효한 숫자를 입력하세요. 거래가 취소됩니다.")
            TradeManager.cancel(player)
            return
        }

        val session = TradeManager.getSession(player) ?: run {
            player.sendMessage("§c거래 세션이 없습니다.")
            return
        }
        // XP 금액 설정
        session.getOffer(player).xp = amount
        player.sendMessage("§aXP $amount 예치 완료.")

        // GUI 재오픈 플래그 설정
        TradeManager.pendingReopen.add(session.requester.uniqueId)
        TradeManager.pendingReopen.add(session.accepter.uniqueId)

        // 메인 스레드에서 GUI 강제 재오픈
        Bukkit.getScheduler().runTask(RPGCore.INSTANCE, Runnable {
            TradeManager.reopenGui(session)
        })
    }

    /**
     * 거래창 닫힘 처리: XP 예치 또는 재오픈일 때만 무시, 그 외는 거래 취소
     */
    @EventHandler
    fun onClose(e: InventoryCloseEvent) {
        if (e.view.title != "§6§l거래 중") return
        val player = e.player as Player

        // XP 예치용 닫힘 무시
        if (TradeManager.pendingXpInput.contains(player.uniqueId)) return
        // 재오픈 모드 무시 및 플래그 제거
        if (TradeManager.pendingReopen.remove(player.uniqueId)) return

        // 그 외에는 거래 취소
        TradeManager.cancel(player)
    }
}

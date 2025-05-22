// src/main/kotlin/org/flash/rpgcore/trade/TradeManager.kt
package org.flash.rpgcore.trade

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.*

object TradeManager {
    // player UUID → TradeSession
    private val sessions = mutableMapOf<UUID, TradeSession>()

    // XP 입력 대기 중인 플레이어
    val pendingXpInput = mutableSetOf<UUID>()
    /** GUI 재오픈용 대기 플래그 */
    val pendingReopen = mutableSetOf<UUID>()

    /** 현재 거래 중인지 */
    fun isTrading(p: Player): Boolean =
        sessions.containsKey(p.uniqueId)

    /** 해당 플레이어의 세션 */
    fun getSession(p: Player): TradeSession? =
        sessions[p.uniqueId]

    /**
     * 거래 요청: 양쪽이 모두 비거래 상태여야 성공
     * @return 성공 여부
     */
    fun request(sender: Player, target: Player): Boolean {
        if (isTrading(sender) || isTrading(target) || sender == target) return false
        val session = TradeSession(sender, target)
        sessions[sender.uniqueId] = session
        sessions[target.uniqueId] = session
        return true
    }

    /**
     * 거래 수락: 반드시 requester → accepter 순서여야 자동 GUI 진입
     * @return 성공 여부
     */
    fun accept(accepter: Player, requester: Player): Boolean {
        val session = sessions[accepter.uniqueId] ?: return false
        if (session.requester != requester || session.accepter != accepter) return false

        // GUI 생성 및 양측에 열기
        val inv = TradeGUI.create(session)
        session.requester.openInventory(inv)
        session.accepter.openInventory(inv)
        return true
    }

    fun reopenGui(session: TradeSession) {
        val inv = TradeGUI.create(session)
        session.requester.openInventory(inv)
        session.accepter .openInventory(inv)
    }

    /**
     * 거래 취소: 세션 제거
     */
    fun cancel(p: Player) {
        val session = sessions[p.uniqueId] ?: return
        sessions.remove(session.requester.uniqueId)
        sessions.remove(session.accepter.uniqueId)
        pendingXpInput.remove(session.requester.uniqueId)
        pendingXpInput.remove(session.accepter.uniqueId)
        session.requester.sendMessage("§c거래가 취소되었습니다.")
        session.accepter .sendMessage("§c거래가 취소되었습니다.")
    }

    /**
     * 거래 완료: 아이템·XP 교환 후 세션 제거
     */
    fun finalize(session: TradeSession) {
        val p1 = session.requester
        val p2 = session.accepter
        // 1) 아이템 교환
        session.offerRequest.items.forEach { p2.inventory.addItem(it) }
        session.offerAccept.items .forEach { p1.inventory.addItem(it) }

        // 2) XP 교환
        val xp1 = session.offerRequest.xp
        val xp2 = session.offerAccept.xp
        if (xp1 > 0) {
            p1.giveExp(-xp1)
            p2.giveExp( xp1)
        }
        if (xp2 > 0) {
            p2.giveExp(-xp2)
            p1.giveExp( xp2)
        }

        // 3) GUI 닫고 안내
        p1.closeInventory(); p2.closeInventory()
        p1.sendMessage("§a거래가 완료되었습니다.")
        p2.sendMessage("§a거래가 완료되었습니다.")

        // 4) 세션 제거
        sessions.remove(p1.uniqueId)
        sessions.remove(p2.uniqueId)
    }
}

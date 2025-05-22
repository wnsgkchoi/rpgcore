// src/main/kotlin/org/flash/rpgcore/trade/TradeSession.kt
package org.flash.rpgcore.trade

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** 한 플레이어의 제안(아이템, XP, 수락 여부) */
data class Offer(
    val items: MutableList<ItemStack> = mutableListOf(),
    var xp: Int = 0,
    var accepted: Boolean = false
)

/** 두 플레이어 간 거래 세션 */
class TradeSession(val requester: Player, val accepter: Player) {
    val offerRequest = Offer()
    val offerAccept  = Offer()

    /** player 에 해당하는 Offer 반환 */
    fun getOffer(p: Player): Offer =
        if (p == requester) offerRequest else offerAccept

    /** player 의 상대방 */
    fun getPartner(p: Player): Player =
        if (p == requester) accepter else requester

    /** 양쪽이 전부 수락했는지 */
    fun bothAccepted(): Boolean =
        offerRequest.accepted && offerAccept.accepted
}

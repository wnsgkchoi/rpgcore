package org.flash.rpgcore.util

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.stats.StatManager

object VirtualHp {
    private val KEY = NamespacedKey(RPGCore.INSTANCE, "virtual_hp")

    /** 현재 가상 HP 가져오기 */
    fun get(p: Player): Int =
        p.persistentDataContainer.getOrDefault(KEY, PersistentDataType.INTEGER, 0)

    /** 가상 HP 설정 (음수나 max 초과 자동 보정) */
    fun set(p: Player, value: Int) {
        val max = StatManager.load(p.uniqueId).finalHP.toInt() - 20
        val v = value.coerceIn(0, max)
        p.persistentDataContainer.set(KEY, PersistentDataType.INTEGER, v)
    }

    /** 초기화: max-20 으로 채움 */
    fun fillMax(p: Player) {
        val maxVirtual = (StatManager.load(p.uniqueId).finalHP.toInt() - 20).coerceAtLeast(0)
        p.persistentDataContainer.set(KEY, PersistentDataType.INTEGER, maxVirtual)
    }
}

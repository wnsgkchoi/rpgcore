package org.flash.rpgcore.util

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.classs.PlayerClass

object ClassWeapons {
    private val KEY = NamespacedKey(RPGCore.INSTANCE, "class_weapon")

    fun make(pc: PlayerClass): ItemStack {
        val mat = when (pc) {
            PlayerClass.BERSERKER -> Material.DIAMOND_AXE
            PlayerClass.TANK      -> Material.DIAMOND_SWORD
            PlayerClass.HITMAN    -> Material.CROSSBOW
            PlayerClass.SNIPER    -> Material.BOW
            PlayerClass.ELEMENTIST      -> Material.DIAMOND_SHOVEL
            PlayerClass.NOVICE    -> Material.AIR
        }
        return ItemStack(mat).apply {
            val m: ItemMeta = itemMeta
            m.setUnbreakable(true)
            m.persistentDataContainer.set(KEY, PersistentDataType.BYTE, 1)
            itemMeta = m
        }
    }

    fun isClassWeapon(it: ItemStack?): Boolean =
        it?.itemMeta?.persistentDataContainer?.has(KEY, PersistentDataType.BYTE) == true
}

package org.flash.rpgcore.equipment

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID

class EquipHolder(val owner: UUID) : InventoryHolder {
    private lateinit var inv: Inventory
    override fun getInventory(): Inventory = inv
    fun setInventory(i: Inventory) { inv = i }
}
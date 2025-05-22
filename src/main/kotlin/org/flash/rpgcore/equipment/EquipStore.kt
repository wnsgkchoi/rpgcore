// File: EquipStore.kt
package org.flash.rpgcore.equipment

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.flash.rpgcore.RPGCore
import java.io.File
import java.util.UUID

class EquipStore(private val plugin: RPGCore) {

    private fun file(uuid: UUID): File =
        File(plugin.dataFolder, "equip/${uuid}.yml")

    /** 저장: null 이면 슬롯 비움, 아니면 ItemStack 저장 :contentReference[oaicite:2]{index=2}:contentReference[oaicite:3]{index=3} */
    fun saveItem(uuid: UUID, key: String, item: ItemStack?) {
        val f   = file(uuid)
        val cfg = if (f.exists()) YamlConfiguration.loadConfiguration(f) else YamlConfiguration()

        if (item == null || item.type.isAir) cfg.set("slots.$key", null)
        else cfg.set("slots.$key", item)

        f.parentFile.mkdirs()
        cfg.save(f)
    }

    /** 로드: 슬롯 키에 해당하는 ItemStack 반환 :contentReference[oaicite:4]{index=4}:contentReference[oaicite:5]{index=5} */
    fun loadItem(uuid: UUID, key: String): ItemStack? {
        val f = file(uuid)
        if (!f.exists()) return null
        val cfg = YamlConfiguration.loadConfiguration(f)
        return cfg.getItemStack("slots.$key")
    }

    companion object {
        fun saveItem(uuid: UUID, key: String, item: ItemStack?) =
            RPGCore.INSTANCE.equipStore.saveItem(uuid, key, item)
        fun loadItem(uuid: UUID, key: String): ItemStack? =
            RPGCore.INSTANCE.equipStore.loadItem(uuid, key)
    }
}

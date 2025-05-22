package org.flash.rpgcore.classs

import org.flash.rpgcore.RPGCore
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

object ClassManager {
    private val cache = mutableMapOf<UUID, PlayerClass>()
    private val folder get() = File(RPGCore.INSTANCE.dataFolder, "class")

    fun get(uuid: UUID): PlayerClass = cache[uuid] ?: load(uuid)

    fun set(uuid: UUID, pc: PlayerClass) {
        cache[uuid] = pc
        save(uuid, pc)
    }

    private fun load(uuid: UUID): PlayerClass {
        val f = File(folder, "$uuid.yml")
        if (!f.exists()) return PlayerClass.NOVICE
        val cfg = YamlConfiguration.loadConfiguration(f)
        val name = cfg.getString("class") ?: return PlayerClass.NOVICE
        return runCatching { PlayerClass.valueOf(name) }.getOrElse { PlayerClass.NOVICE }
            .also { cache[uuid] = it }
    }

    private fun save(uuid: UUID, pc: PlayerClass) {
        folder.mkdirs()
        val f = File(folder, "$uuid.yml")
        val cfg = YamlConfiguration()
        cfg.set("class", pc.name)
        cfg.save(f)
    }
}

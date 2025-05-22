// File: SkillStore.kt
package org.flash.rpgcore.skills

import org.bukkit.configuration.file.YamlConfiguration
import org.flash.rpgcore.RPGCore
import java.io.File
import java.util.*

object SkillStore {
    private fun getFile(uuid: UUID): File {
        val dir = File(RPGCore.INSTANCE.dataFolder, "skills")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$uuid.yml")
    }

    private fun loadConfig(uuid: UUID): YamlConfiguration {
        val file = getFile(uuid)
        if (!file.exists()) file.createNewFile()
        return YamlConfiguration.loadConfiguration(file)
    }

    /** 학습(learned)된 스킬의 레벨 맵을 저장 */
    fun saveLearnedLevels(uuid: UUID, learned: Map<String, Int>) {
        val cfg = loadConfig(uuid)
        cfg.set("learned", null) // 초기화
        learned.forEach { (id, lvl) ->
            cfg.set("learned.$id", lvl)
        }
        cfg.save(getFile(uuid))
    }

    /** 학습(learned)된 스킬의 레벨 맵을 불러옴 */
    fun loadLearnedLevels(uuid: UUID): MutableMap<String, Int> {
        val cfg     = loadConfig(uuid)
        val section = cfg.getConfigurationSection("learned") ?: return mutableMapOf()
        return section.getKeys(false)
            .associateWith { section.getInt(it, 1) }
            .toMutableMap()
    }

    /** 슬롯(slot1~3)에 장착된 스킬 ID/Level 저장 */
    fun saveSkill(uuid: UUID, slot: Int, skillId: String?, level: Int) {
        val cfg  = loadConfig(uuid)
        val path = "slot$slot"
        cfg.set("$path.id",    skillId)
        cfg.set("$path.level", level)
        cfg.save(getFile(uuid))
    }

    fun loadSkillId(uuid: UUID, slot: Int): String? =
        loadConfig(uuid).getString("slot$slot.id")

    fun loadSkillLevel(uuid: UUID, slot: Int): Int =
        loadConfig(uuid).getInt("slot$slot.level", 0)
}

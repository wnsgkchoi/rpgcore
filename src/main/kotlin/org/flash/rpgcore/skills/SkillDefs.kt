// File: SkillDefs.kt
package org.flash.rpgcore.skills

import org.bukkit.configuration.file.YamlConfiguration
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.classs.PlayerClass
import java.io.File

/**
 * skill-defs.yml 을 로드하여 SkillDefinition 인스턴스를 관리합니다.
 * 리로드 시 /rpgcore reload → SkillDefs.reload() 를 호출하세요.
 */
object SkillDefs {
    private val defs = mutableMapOf<String, SkillDefinition>()

    /** 플러그인 시작 또는 `/rpgcore reload` 시 반드시 호출 */
    fun reload() {
        defs.clear()
        val file = File(RPGCore.INSTANCE.dataFolder, "skill-defs.yml")
        if (!file.exists()) RPGCore.INSTANCE.saveResource("skill-defs.yml", false)
        val cfg = YamlConfiguration.loadConfiguration(file)

        for (id in cfg.getKeys(false)) {
            val sec = cfg.getConfigurationSection(id) ?: continue

            val displayName   = sec.getString("display_name")!!
            val description   = sec.getStringList("description")
            val classes       = sec.getStringList("allowed_classes")
                .map { PlayerClass.valueOf(it.uppercase()) }
                .toSet()
            val maxLevel      = sec.getInt("max_level")
            // — 플로우 스타일 & 블록 스타일 모두 지원 —
            val mpCost        = sec.getIntegerList("use_mp")
            val atkCoef       = sec.getDoubleList("atk_coefficient")
            val magCoef       = sec.getDoubleList("mag_coefficient")
            val xpCost        = sec.getIntegerList("use_xp")
            val sucRate       = sec.getDoubleList("suc_rate")
            val cooldownSec   = sec.getDoubleList("cooltime")

            defs[id] = SkillDefinition(
                id             = id,
                displayName    = displayName,
                description    = description,
                allowedClasses = classes,
                maxLevel       = maxLevel,
                mpCost         = mpCost,
                atkCoef        = atkCoef,
                magCoef        = magCoef,
                xpCost         = xpCost,
                sucRate        = sucRate,
                cooldownSec    = cooldownSec
            )
        }

        RPGCore.INSTANCE.logger.info("§a[SkillDefs] Loaded ${defs.size} skills")
    }

    /** ID로 단일 스킬 조회 */
    fun get(id: String): SkillDefinition? = defs[id]
    /** 모든 스킬 정의 반환 */
    fun all(): Collection<SkillDefinition> = defs.values
}

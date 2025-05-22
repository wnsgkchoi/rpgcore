// File: EquipmentDefs.kt
package org.flash.rpgcore.equipment

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.classs.PlayerClass
import java.io.File

object EquipmentDefs {
    private val defs = mutableMapOf<String, EquipmentDef>()

    /** 플러그인 시작 시나 reload 시 호출하여 YML을 다시 읽어옵니다 */
    fun reload() {
        defs.clear()
        val file = File(RPGCore.INSTANCE.dataFolder, "equip-defs.yml")
        if (!file.exists()) {
            RPGCore.INSTANCE.saveResource("equip-defs.yml", false)
        }
        val cfg = YamlConfiguration.loadConfiguration(file)

        for (key in cfg.getKeys(false)) { // 각 아이템 ID 루프
            val sec = cfg.getConfigurationSection(key)!!
            val id = key // 아이템 ID (올바르게 할당되었는지 확인)
            val name = sec.getString("name")!! // 이름 (올바르게 할당되었는지 확인)
            val desc = sec.getStringList("description")
            val mat = Material.valueOf(sec.getString("material")!!.uppercase())
            val slot = EquipSlot.valueOf(sec.getString("slot")!!.uppercase())
            val maxEnhance  = sec.getInt("max_enhance", 0) // 기본값 0 추가

            val validClassesStrings = sec.getStringList("validClasses")

            val validClassesEnums = validClassesStrings
                .mapNotNull { className ->
                    try {
                        PlayerClass.valueOf(className.trim().uppercase()) // 공백 제거 및 대문자 변환
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            val cooldownMsFromFile = sec.getLong("cooldownMs", 0L)

            val xpCost      = sec.getIntegerList("xp_cost")
            val successRate = sec.getDoubleList("success_rate").map { it / 100.0 }

            // --- stats_per_level 읽기 ---
            val statsSection  = sec.getConfigurationSection("stats_per_level")!!

            val listFlatHp    = statsSection.getIntegerList("flat_hp")
            val listFlatMp    = statsSection.getIntegerList("flat_mp")
            val listFlatAtk   = statsSection.getIntegerList("flat_atk")
            val listFlatDef   = statsSection.getIntegerList("flat_def")
            val listFlatMagic = statsSection.getIntegerList("flat_magic")
            val listFlatMdef  = statsSection.getIntegerList("flat_mdef")

            val listHpPct     = statsSection.getDoubleList("hp_pct")
            val listMpPct     = statsSection.getDoubleList("mp_pct")
            val listAtkPct    = statsSection.getDoubleList("atk_pct")
            val listDefPct    = statsSection.getDoubleList("def_pct")
            val listMagicPct  = statsSection.getDoubleList("magic_pct")
            val listMdefPct   = statsSection.getDoubleList("mdef_pct")

            // 0강부터 maxEnhance강까지 총 (maxEnhance+1)개의 StatBlock 생성
            val statsList = (0..maxEnhance).map { lvl ->
                EquipmentDef.StatBlock(
                    flatHp    = listFlatHp.getOrElse(lvl)    { 0 },
                    hpPct     = listHpPct.getOrElse(lvl)     { 0.0 },
                    flatMp    = listFlatMp.getOrElse(lvl)    { 0 },
                    mpPct     = listMpPct.getOrElse(lvl)     { 0.0 },
                    flatAtk   = listFlatAtk.getOrElse(lvl)   { 0 },
                    atkPct    = listAtkPct.getOrElse(lvl)    { 0.0 },
                    flatDef   = listFlatDef.getOrElse(lvl)   { 0 },
                    defPct    = listDefPct.getOrElse(lvl)    { 0.0 },
                    flatMagic = listFlatMagic.getOrElse(lvl) { 0 },
                    magicPct  = listMagicPct.getOrElse(lvl)  { 0.0 },
                    flatMdef  = listFlatMdef.getOrElse(lvl)  { 0 },
                    mdefPct   = listMdefPct.getOrElse(lvl)   { 0.0 },
                )
            }

            defs[id] = EquipmentDef(
                id             = id,
                displayName    = name,
                description    = desc,
                material       = mat,
                slot           = slot,
                maxEnhance     = maxEnhance,
                xpCost         = xpCost,
                successRate    = successRate,
                statsPerLevel  = statsList,
                validClasses   = validClassesEnums,
                cooldownMs     = cooldownMsFromFile
            )
        }

        println("[EquipmentDefs DEBUG] Finished reloading all equipment definitions. Total items loaded: ${defs.size}")
        // reload 시 이미 접속 중인 플레이어들의 장착 아이템도 새 정의로 갱신
        EquipManager.refreshAllPlayers()
    }

    fun get(id: String): EquipmentDef? = defs[id]
    fun all(): Collection<EquipmentDef> = defs.values
}

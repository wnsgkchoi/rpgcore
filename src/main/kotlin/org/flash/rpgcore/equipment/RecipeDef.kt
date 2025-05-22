package org.flash.rpgcore.equipment

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import org.flash.rpgcore.RPGCore
import java.io.File

data class Ingredient(
    val mat: Material? = null,
    val equipId: String? = null,
    val amount: Int
)

/**
 * 레시피 정의: 어떤 레시피(id)가
 *  - equipId 결과 장비를 만들고
 *  - main 재료(장비 시스템 아이템이어야 함)
 *  - subs 보조 재료(매트 또는 equipId)
 *  - xp 경험치 비용
 */
data class RecipeDef(
    val id: String,
    val equipId: String,
    val main: Ingredient,
    val subs: List<Ingredient>,
    val xp: Int
)

object RecipeDefs {

    private val defs = mutableMapOf<String, RecipeDef>()

    /** 서버 시작 또는 reload 시 한 번 호출 */
    fun load() {
        val plugin = RPGCore.INSTANCE
        val file = File(plugin.dataFolder, "recipe-defs.yml")
        if (!file.exists()) {
            plugin.saveResource("recipe-defs.yml", false)
        }
        val cfg = YamlConfiguration.loadConfiguration(file)

        defs.clear()
        for (key in cfg.getKeys(false)) {
            val sec = cfg.getConfigurationSection(key)
                ?: throw IllegalArgumentException("recipe-defs.yml: 섹션 $key 가 없습니다")

            // 1) 결과 장비 ID
            val resultEquipId = sec.getString("equipId")
                ?: throw IllegalArgumentException("Recipe $key: equipId 가 없습니다")
            if (EquipmentDefs.get(resultEquipId) == null) {
                throw IllegalArgumentException("Recipe $key: 알 수 없는 결과 equipId=$resultEquipId")
            }

            // 2) main 재료 (장비 시스템 아이템이어야 함)
            val mainSec = sec.getConfigurationSection("main")
                ?: throw IllegalArgumentException("Recipe $key: main 섹션이 없습니다")
            val mainIng = mainSec.toIngredient()
            if (mainIng.equipId == null) {
                throw IllegalArgumentException("Recipe $key: main 은 반드시 equipId 로 지정해야 합니다")
            }
            if (EquipmentDefs.get(mainIng.equipId) == null) {
                throw IllegalArgumentException("Recipe $key: main 알 수 없는 equipId=${mainIng.equipId}")
            }

            // 3) subs 재료 (mat 또는 equipId)
            val subs = sec.getMapList("subs").map { raw ->
                @Suppress("UNCHECKED_CAST")
                (raw as Map<*, *>).toIngredient()
            }

            // 4) xp 비용
            val xpCost = sec.getInt("xp", 0)

            defs[key] = RecipeDef(
                id      = key,
                equipId = resultEquipId,
                main    = mainIng,
                subs    = subs,
                xp      = xpCost
            )
        }

        plugin.logger.info("§a[RecipeDefs] Loaded ${defs.size} recipes")
    }

    fun get(id: String): RecipeDef? = defs[id]
    fun all(): Collection<RecipeDef> = defs.values

    // ───────── YAML → Ingredient 헬퍼 ─────────
    private fun Map<*, *>.toIngredient(): Ingredient {
        val eid = (this["equipId"] as? String)?.takeIf { it.isNotBlank() }
        val mat = if (eid == null) (this["mat"] as? String)?.let {
            try {
                Material.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Ingredient mat 값 '$it' 은 올바른 Material 이 아닙니다")
            }
        } else null

        if (eid == null && mat == null) {
            throw IllegalArgumentException("Ingredient 에서 mat 또는 equipId 중 하나는 반드시 지정해야 합니다")
        }
        if (eid != null && EquipmentDefs.get(eid) == null) {
            throw IllegalArgumentException("Ingredient unknown equipId='$eid'")
        }

        val amtAny = this["amount"]
            ?: throw IllegalArgumentException("Ingredient amount 가 없습니다")
        val amt = (amtAny as? Number)?.toInt()
            ?: throw IllegalArgumentException("Ingredient amount '$amtAny' 은 숫자가 아닙니다")

        return Ingredient(mat = mat, equipId = eid, amount = amt)
    }

    private fun ConfigurationSection.toIngredient() =
        this.getValues(false).toIngredient()
}
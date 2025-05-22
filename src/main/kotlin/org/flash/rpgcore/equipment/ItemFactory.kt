package org.flash.rpgcore.equipment

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.RPGCore

object ItemFactory {
    private val plugin = RPGCore.INSTANCE
    // LEGACY_SERIALIZER 선언
    private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()

    /**
     * EquipmentDef 에 정의된 장비(def)와
     * 현재 강화 레벨(level: 0..maxEnhance)을 받아,
     * PDC, 이름, 루어까지 모두 세팅된 ItemStack을 반환
     */
    fun create(def: EquipmentDef, level: Int): ItemStack {
        require(level in 0..def.maxEnhance) {
            "level must be 0..${def.maxEnhance}"
        }

        // (1) 아이템 + 메타
        val item = ItemStack(def.material, 1)
        val meta = item.itemMeta ?: return item

        // (2) PDC: ID / 강화 레벨
        val pdc   = meta.persistentDataContainer
        pdc.set(ItemKeys.EQUIP_ID,  PersistentDataType.STRING,  def.id)
        pdc.set(ItemKeys.ENH_LEVEL, PersistentDataType.INTEGER, level)

        // (3) PDC: 스탯 값들도 모두 기록!
        val stats = def.statsPerLevel[level]
        with(pdc) {
            set(ItemKeys.HP,     PersistentDataType.INTEGER, stats.flatHp)
            set(ItemKeys.HP_PCT, PersistentDataType.DOUBLE,  stats.hpPct)
            set(ItemKeys.MP,     PersistentDataType.INTEGER, stats.flatMp)
            set(ItemKeys.MP_PCT, PersistentDataType.DOUBLE,  stats.mpPct)
            set(ItemKeys.ATK,    PersistentDataType.INTEGER, stats.flatAtk)
            set(ItemKeys.ATK_PCT, PersistentDataType.DOUBLE,  stats.atkPct)
            set(ItemKeys.DEF,    PersistentDataType.INTEGER, stats.flatDef)
            set(ItemKeys.DEF_PCT, PersistentDataType.DOUBLE,  stats.defPct)
            set(ItemKeys.SPA,    PersistentDataType.INTEGER, stats.flatMagic)
            set(ItemKeys.SPA_PCT, PersistentDataType.DOUBLE,  stats.magicPct)
            set(ItemKeys.SPD,    PersistentDataType.INTEGER, stats.flatMdef)
            set(ItemKeys.SPD_PCT, PersistentDataType.DOUBLE,  stats.mdefPct)
        }

        // (4) 디스플레이 이름
        meta.displayName(LEGACY_SERIALIZER.deserialize(def.displayName))

        // (5) 루어
        val lore = mutableListOf<Component>()
        if (def.description.isNotEmpty()) {
            def.description.forEach { line ->
                // 설명(description)도 레거시 코드를 포함할 수 있다면 deserialize 사용
                lore += LEGACY_SERIALIZER.deserialize(line)
            }
            lore += Component.empty() // 설명과 스탯 사이에 빈 줄 추가
        }


        fun appendStat(
            label: String,
            flat: Int,
            pct: Double,
            cFlat: NamedTextColor,
            cPct: NamedTextColor
        ) {
            if (flat != 0 || pct != 0.0) {
                val p = (pct * 100).toInt()
                val statLine = Component.text() // 빌더 시작
                    .append(LEGACY_SERIALIZER.deserialize("§7$label: ")) // 라벨 (회색)
                    .append(Component.text((if (flat >= 0) "+" else "") + flat.toString(), cFlat)) // 플랫 값
                if (pct != 0.0) { // 퍼센트 값이 있을 때만 표시
                    statLine.append(Component.text(" (${(if (p >= 0) "+" else "")}$p%)", cPct)) // 퍼센트 값
                }
                lore += statLine.build()
            }
        }

        appendStat("체력",   stats.flatHp,    stats.hpPct,    NamedTextColor.BLUE,        NamedTextColor.BLUE)
        appendStat("MP",     stats.flatMp,    stats.mpPct,    NamedTextColor.AQUA,        NamedTextColor.AQUA)
        appendStat("공격력", stats.flatAtk,   stats.atkPct,   NamedTextColor.RED,         NamedTextColor.RED)
        appendStat("방어력", stats.flatDef,   stats.defPct,   NamedTextColor.GREEN,       NamedTextColor.GREEN)
        appendStat("마력",   stats.flatMagic, stats.magicPct, NamedTextColor.LIGHT_PURPLE,NamedTextColor.LIGHT_PURPLE)
        appendStat("마방",   stats.flatMdef,  stats.mdefPct,  NamedTextColor.DARK_PURPLE, NamedTextColor.DARK_PURPLE)

        // (6) 다음 강화 미리보기 (최대 강화 단계에서는 건너뜀)
        if (level < def.maxEnhance) {
            val next      = level + 1
            val nxtStats  = def.statsPerLevel[next]
            val xpCost    = def.xpCost[next]
            val sucPct    = (def.successRate[next] * 100).toInt()

            lore += Component.empty()
            lore += Component.text("→ 다음 강화: $level → $next", NamedTextColor.YELLOW)
            lore += Component.text("필요 XP: $xpCost", NamedTextColor.GRAY)
            lore += Component.text("성공 확률: ${sucPct}%", NamedTextColor.GRAY)

            val preview = mutableListOf<String>()
            fun delta(label: String, curFlat: Int, curPct: Double, nFlat: Int, nPct: Double) {
                val df = nFlat - curFlat
                val dp = ((nPct - curPct) * 100).toInt()
                if (df != 0 || dp != 0) {
                    val f = if (df!=0) "${if (df>0) "+" else ""}$df" else ""
                    val p = if (dp!=0) " (${if (dp>0) "+" else ""}$dp%)" else ""
                    preview += "$label $f$p"
                }
            }

            delta("체력",   stats.flatHp,    stats.hpPct,    nxtStats.flatHp,    nxtStats.hpPct)
            delta("MP",     stats.flatMp,    stats.mpPct,    nxtStats.flatMp,    nxtStats.mpPct)
            delta("공격력", stats.flatAtk,   stats.atkPct,   nxtStats.flatAtk,   nxtStats.atkPct)
            delta("방어력", stats.flatDef,   stats.defPct,   nxtStats.flatDef,   nxtStats.defPct)
            delta("마력",   stats.flatMagic, stats.magicPct, nxtStats.flatMagic, nxtStats.magicPct)
            delta("마방",   stats.flatMdef,  stats.mdefPct,  nxtStats.flatMdef,  nxtStats.mdefPct)

            if (preview.isNotEmpty()) {
                lore += Component.text("추가: ${preview.joinToString(", ")}", NamedTextColor.GRAY)
            }
        }

        meta.lore(lore)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
        meta.isUnbreakable = true
        item.itemMeta = meta
        return item
    }
}

package org.flash.rpgcore.equipment

import org.bukkit.NamespacedKey
import org.flash.rpgcore.RPGCore

object ItemKeys {
    // ── 장비 식별 및 강화 상태 ─────────────────────────────
    /** 어떤 EquipmentDef 인지 저장하는 키 */
    val EQUIP_ID     = NamespacedKey(RPGCore.INSTANCE, "equip_id")
    /** 현재 강화 레벨 0~maxLevel */
    val ENH_LEVEL    = NamespacedKey(RPGCore.INSTANCE, "enh_level")
    val EQUIP_COOLDOWN  = NamespacedKey(RPGCore.INSTANCE, "equip_cd")

    // ── 장비 스탯 저장용 키 ────────────────────────────────
    /** 추가 체력(flat) */
    val HP      = NamespacedKey(RPGCore.INSTANCE, "hp")
    /** 추가 체력(%) */
    val HP_PCT  = NamespacedKey(RPGCore.INSTANCE, "hp_pct")
    /** 추가 마나(flat) */
    val MP      = NamespacedKey(RPGCore.INSTANCE, "mp")
    /** 추가 마나(%) */
    val MP_PCT  = NamespacedKey(RPGCore.INSTANCE, "mp_pct")
    /** 추가 공격력(flat) */
    val ATK     = NamespacedKey(RPGCore.INSTANCE, "atk")
    /** 추가 공격력(%) */
    val ATK_PCT = NamespacedKey(RPGCore.INSTANCE, "atk_pct")
    /** 추가 방어력(flat) */
    val DEF     = NamespacedKey(RPGCore.INSTANCE, "def")
    /** 추가 방어력(%) */
    val DEF_PCT = NamespacedKey(RPGCore.INSTANCE, "def_pct")
    /** 추가 마력(flat) */
    val SPA     = NamespacedKey(RPGCore.INSTANCE, "spa")
    /** 추가 마력(%) */
    val SPA_PCT = NamespacedKey(RPGCore.INSTANCE, "spa_pct")
    /** 추가 마법방어(flat) */
    val SPD     = NamespacedKey(RPGCore.INSTANCE, "spd")
    /** 추가 마법방어(%) */
    val SPD_PCT = NamespacedKey(RPGCore.INSTANCE, "spd_pct")
}


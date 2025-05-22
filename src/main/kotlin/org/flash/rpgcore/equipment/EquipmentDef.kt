package org.flash.rpgcore.equipment

import org.bukkit.Material
import org.flash.rpgcore.classs.PlayerClass

/**
 * 한 장비(id)에 대한 정의
 */
data class EquipmentDef(
    /** YML상의 key, 고유 ID */
    val id: String,
    /** 인게임에 표시될 이름 (ChatColor 포함 가능) */
    val displayName: String,
    /** 설명(여러 줄) */
    val description: List<String>,
    /** GUI 등에 사용할 아이콘 머테리얼 */
    val material: Material,
    /** 장착 부위 */
    val slot: EquipSlot,
    /** 최대 강화 단계(e.g. 10) */
    val maxEnhance: Int,
    /** 각 레벨에서의 XP 소모량 리스트 (size == maxEnhance) */
    val xpCost: List<Int>,
    /** 각 레벨에서의 강화 성공 확률(0.0~1.0) 리스트 (size == maxEnhance) */
    val successRate: List<Double>,
    /** 각 레벨별 추가 스탯 블록 (index 0 ⇒ +1강, …, index maxEnhance-1 ⇒ +maxEnhance강) */
    val statsPerLevel: List<StatBlock>,
    val validClasses: List<PlayerClass> = emptyList(),
    /** 이 장비를 착용했을 때 적용할 일반공격 쿨타임(ms) */
    val cooldownMs: Long = 0L
) {
    /**
     * 한 단계 강화 시 얻는 스탯
     */
    data class StatBlock(
        val flatHp: Int,    val hpPct: Double,
        val flatMp: Int,    val mpPct: Double,
        val flatAtk: Int,   val atkPct: Double,
        val flatDef: Int,   val defPct: Double,
        val flatMagic: Int, val magicPct: Double,
        val flatMdef: Int,  val mdefPct: Double,
    )
}

/** 장착 가능한 부위 열거 */
enum class EquipSlot {
    WEAPON, HELMET, CHEST, LEGS, BOOTS, RING, NECKLACE, BRACELET, CAPE, GLOVES, EARRING, BELT
}

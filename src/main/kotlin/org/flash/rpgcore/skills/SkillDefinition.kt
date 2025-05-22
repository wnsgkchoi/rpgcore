// File: SkillDefinition.kt
package org.flash.rpgcore.skills

import org.flash.rpgcore.classs.PlayerClass

/**
 * skill-defs.yml 에 정의된 한 스킬의 정보
 *
 * @property id             스킬 고유 ID (YAML 키)
 * @property displayName    인게임에 표시될 이름 (색코드 포함)
 * @property description    툴팁에 나올 설명 문자열 목록
 * @property allowedClasses 해당 스킬을 사용할 수 있는 클래스 집합
 * @property maxLevel       스킬의 최대 레벨
 * @property mpCost         레벨별 MP 소비량 (크기 = maxLevel+1)
 * @property atkCoef        레벨별 물리 계수 (크기 = maxLevel+1)
 * @property magCoef        레벨별 마법 계수 (크기 = maxLevel+1)
 * @property xpCost         레벨별 강화에 필요한 XP (크기 = maxLevel+1)
 * @property sucRate        레벨별 강화 성공 확률(0.0~100.0) (크기 = maxLevel+1)
 * @property cooldownSec    레벨별 스킬 쿨타임(초) (크기 = maxLevel+1)
 */
enum class SkillType { SINGLE, AURA, GROUND, DEBUFF, BUFF }

data class SkillDefinition(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val allowedClasses: Set<PlayerClass>,
    val maxLevel: Int,
    val mpCost: List<Int>,
    val atkCoef: List<Double>,
    val magCoef: List<Double>,
    val xpCost: List<Int>,
    val sucRate: List<Double>,
    val cooldownSec: List<Double>
) {
    init {
        require(mpCost.size       == maxLevel + 1) { "mpCost size must be maxLevel+1" }
        require(atkCoef.size      == maxLevel + 1) { "atkCoef size must be maxLevel+1" }
        require(magCoef.size      == maxLevel + 1) { "magCoef size must be maxLevel+1" }
        require(xpCost.size       == maxLevel + 1) { "xpCost size must be maxLevel+1" }
        require(sucRate.size      == maxLevel + 1) { "sucRate size must be maxLevel+1" }
        require(cooldownSec.size  == maxLevel + 1) { "cooldownSec size must be maxLevel+1" }
    }

    /** 현재 레벨(level: 0..maxLevel)에 해당하는 MP 소비량 */
    fun getMpCost(level: Int)      = mpCost[level]
    /** 물리 데미지 계수 */
    fun getAtkCoef(level: Int)     = atkCoef[level]
    /** 마법 데미지 계수 */
    fun getMagCoef(level: Int)     = magCoef[level]
    /** 다음 레벨 강화에 필요한 XP */
    fun getXpCost(level: Int)      = xpCost[level]
    /** 해당 레벨 강화 성공 확률 (0.0~100.0) */
    fun getSucRate(level: Int)     = sucRate[level]
    /** 해당 레벨 스킬 쿨타임(초) */
    fun getCooldown(level: Int)    = cooldownSec[level]
}

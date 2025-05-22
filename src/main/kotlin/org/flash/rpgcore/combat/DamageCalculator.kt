package org.flash.rpgcore.combat

import kotlin.math.max
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.flash.rpgcore.stats.StatManager
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.classs.PlayerClass
import org.flash.rpgcore.util.VirtualHp
import kotlin.math.sqrt
import org.bukkit.inventory.ItemStack
import org.flash.rpgcore.equipment.EquipmentDefs
import org.flash.rpgcore.equipment.ItemKeys
import org.bukkit.persistence.PersistentDataType

object DamageCalculator {
    /**
     * 데미지를 계산합니다.
     * NOVICE의 경우 weaponStack이 null이며, 이 경우 무기 스탯의 영향을 받지 않습니다.
     */
    fun calc(
        attacker: Player,
        weaponStack: ItemStack?, // ItemStack?으로 변경 (NOVICE의 경우 null 가능)
        target: LivingEntity,
        physCoef: Double = 1.0,
        magicCoef: Double = 0.0
    ): Double {
        val attackerStats = StatManager.load(attacker.uniqueId)
        val targetStats = if (target is Player) StatManager.load(target.uniqueId) else null
        val attackerClass = ClassManager.get(attacker.uniqueId)

        var berserkerBonus = 0.0
        if (attackerClass == PlayerClass.BERSERKER) {
            val hpNow = attacker.health + VirtualHp.get(attacker)
            val hpMax = attackerStats.finalHP
            if (hpMax > hpNow) {
                val lostHp = hpMax - hpNow
                berserkerBonus = lostHp * sqrt(lostHp) * 0.05
            }
        }

        // NOVICE이거나 weaponStack이 null (또는 AIR)인 경우, 순수 스탯 기반 공격력/마력 사용
        // 그 외의 경우는 attackerStats.finalAttack/Magic (이미 장비 포함) 사용
        // weaponStack이 null이 아니더라도, finalAttack/Magic은 이미 해당 weaponStack의 스탯을 포함하고 있음.
        // 따라서 weaponStack의 유무는 현재 데미지 계산식에서 직접적인 수치 변화를 주지 않음.
        // (단, weaponStack의 equipId에 따른 특수 효과 등을 나중에 추가한다면 이 부분에서 분기 가능)

        val baseAttack = attackerStats.finalAttack + berserkerBonus
        val targetDefense = targetStats?.finalDefense ?: 0.0

        val baseMagic = attackerStats.finalMagic
        val targetMagicDefense = targetStats?.finalMdef ?: 0.0

        // NOVICE이고 weaponStack이 null이면, physCoef는 기본 공격(1.0)으로 간주, magicCoef는 0.0으로 간주
        // 또는 NOVICE 전용 기본 공격력 값을 사용할 수 있음.
        // 여기서는 다른 클래스와 동일한 계산식을 따르되, finalAttack/Magic이 무기 미착용 상태의 값일 것임.
        // (StatManager.recalcFor에서 NOVICE는 장비 스탯이 0으로 계산될 것이므로)

        val physicalDamage = if (physCoef > 0) {
            physCoef * baseAttack * 50 / (50 + targetDefense)
        } else 0.0

        val magicalDamage = if (magicCoef > 0) {
            magicCoef * baseMagic * 50 / (50 + targetMagicDefense)
        } else 0.0

        val totalDamage = physicalDamage + magicalDamage
        return max(1.0, totalDamage)
    }
}
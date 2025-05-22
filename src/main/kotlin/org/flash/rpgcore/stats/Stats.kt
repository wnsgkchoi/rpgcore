package org.flash.rpgcore.stats

data class Stats(
    var health: Int = 20,
    var attack: Int = 5,
    var defense: Int = 5,
    var magic: Int = 5,
    var mdef: Int = 5,
    var mp: Int = 50,
    var finalHP: Double = 20.0,
    var finalMP: Double = 50.0,
    var finalAttack:  Double = 5.0,
    var finalDefense: Double = 5.0,
    var finalMagic: Double = 5.0,
    var finalMdef: Double = 5.0
) {
    fun get(type: StatType) = when (type) {
        StatType.HEALTH  -> health
        StatType.ATTACK  -> attack
        StatType.DEFENSE -> defense
        StatType.MAGIC   -> magic
        StatType.MDEF    -> mdef
        StatType.MP -> mp
    }
    fun add(type: StatType, delta: Int) = when (type) {
        StatType.HEALTH  -> run { health += delta }
        StatType.ATTACK  -> run { attack += delta }
        StatType.DEFENSE -> run { defense += delta }
        StatType.MAGIC   -> run { magic += delta }
        StatType.MDEF    -> run { mdef += delta }
        StatType.MP -> run { mp += delta }
    }
}

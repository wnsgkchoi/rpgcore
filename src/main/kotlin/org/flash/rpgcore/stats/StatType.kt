package org.flash.rpgcore.stats

enum class StatType(val row: Int, val display: String) {
    HEALTH(0, "체력"),
    ATTACK(1, "공격력"),
    DEFENSE(2, "방어력"),
    MAGIC(3, "마력"),
    MDEF(4, "마법방어력"),
    MP(5, "MP")
}

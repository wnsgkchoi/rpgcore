package org.flash.rpgcore.stats

import org.bukkit.Material
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.sidebar.SidebarService      // ★ 추가
import org.bukkit.attribute.Attribute                // ★ 추가
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.bukkit.persistence.PersistentDataType.DOUBLE
import org.bukkit.persistence.PersistentDataType.INTEGER
import org.flash.rpgcore.equipment.EquipStore
import org.flash.rpgcore.equipment.ItemKeys
import org.flash.rpgcore.util.VirtualHp
import org.flash.rpgcore.util.VirtualMp
import java.io.File
import java.util.*

object StatManager {
    private val cache = mutableMapOf<UUID, Stats>()
    private val folder get() = File(RPGCore.INSTANCE.dataFolder, "stats")

    /* ---------- 로드 ---------- */
    fun load(uuid: UUID): Stats {
        cache[uuid]?.let { return it }
        val file = File(folder, "$uuid.yml")
        if (!file.exists()) return Stats().also { cache[uuid] = it }

        val cfg = YamlConfiguration.loadConfiguration(file)
        return Stats(
            cfg.getInt("health", 20),
            cfg.getInt("attack", 5),
            cfg.getInt("defense", 5),
            cfg.getInt("magic", 5),
            cfg.getInt("mdef", 5),
            cfg.getInt("mp", 50)
        ).also { cache[uuid] = it }
    }

    /* ---------- 저장 ---------- */
    fun save(uuid: UUID) {
        val s = cache[uuid] ?: return
        folder.mkdirs()
        val f = File(folder, "$uuid.yml")
        val cfg = YamlConfiguration()
        cfg.set("health",  s.health)
        cfg.set("attack",  s.attack)
        cfg.set("defense", s.defense)
        cfg.set("magic",   s.magic)
        cfg.set("mdef",    s.mdef)
        cfg.set("mp",      s.mp)
        cfg.save(f)
    }

    fun clear(uuid: UUID) {
        cache.remove(uuid)
    }

    /* ---------- 스탯 강화 ---------- */
    fun tryIncrease(p: Player, type: StatType): Boolean {
        val stats = load(p.uniqueId)

        val (base, incPer) = when (type) {
            StatType.HEALTH -> 20 to 2
            StatType.MP     -> 50 to 5      // ★ MP 기본 50, +5씩 증가
            StatType.ATTACK,
            StatType.DEFENSE,
            StatType.MAGIC,
            StatType.MDEF   -> 5  to 1
        }

        val already = (stats.get(type) - base) / incPer
        val cost = (already + 1) * 50                       // 50·100·150…

        if (p.totalExperience < cost) {
            p.sendActionBar(msg("§cXP가 부족합니다: 필요 $cost, 현재 ${p.totalExperience}"))
            return false
        }

        p.giveExp(-cost)                                    // XP 포인트 차감
        stats.add(type, incPer)
        updatePlayerAttributes(p, stats)
        if (type == StatType.HEALTH) {
            VirtualHp.fillMax(p)                  // ★ 체력 상한이 늘어난 만큼 버퍼 확장
        }
        if (type == StatType.MP) {
            VirtualMp.fillMax(p)                  // ★ MP 상한이 늘어난 만큼 버퍼 확장
        }
        save(p.uniqueId)
        recalcFor(p)
        SidebarService.updateNow(p)
        p.sendActionBar(msg("§a${type.display} +$incPer (XP −$cost)"))
        return true
    }

    /* ---------- 체력 Attribute ---------- */
    fun updatePlayerAttributes(p: Player, stats: Stats = load(p.uniqueId)) {
        val attr = p.getAttribute(Attribute.MAX_HEALTH) ?: return
        attr.baseValue = minOf(20.0, stats.health.toDouble())
        if (p.health > attr.value) p.health = attr.value
        save(p.uniqueId)
    }

    fun recalcFor(p: Player) {
        val uuid = p.uniqueId
        val s    = load(uuid)

        var flatHP  = 0
        var flatMP  = 0
        var flatAtk = 0
        var flatDef = 0
        var flatSpA = 0
        var flatSpD = 0
        var pctHP   = 0.0
        var pctMP   = 0.0
        var pctAtk  = 0.0
        var pctDef  = 0.0
        var pctSpA  = 0.0
        var pctSpD  = 0.0

        listOf("weapon","helmet","chest","legs","boots",
            "ring","necklace","bracelet","cape","gloves","earring","belt")
            .forEach { key ->
                val item = EquipStore.loadItem(uuid, key)
                if (item == null || item.type == Material.AIR) return@forEach
                val pdc = item.itemMeta?.persistentDataContainer ?: return@forEach

                flatHP  += pdc.getOrDefault(ItemKeys.HP,     PersistentDataType.INTEGER, 0)
                flatMP  += pdc.getOrDefault(ItemKeys.MP,     PersistentDataType.INTEGER, 0)  // ← MP 집계 수정
                flatAtk += pdc.getOrDefault(ItemKeys.ATK,    PersistentDataType.INTEGER, 0)
                flatDef += pdc.getOrDefault(ItemKeys.DEF,    PersistentDataType.INTEGER, 0)
                flatSpA += pdc.getOrDefault(ItemKeys.SPA,    PersistentDataType.INTEGER, 0)
                flatSpD += pdc.getOrDefault(ItemKeys.SPD,    PersistentDataType.INTEGER, 0)

                pctHP   += pdc.getOrDefault(ItemKeys.HP_PCT,  PersistentDataType.DOUBLE,  0.0)
                pctMP   += pdc.getOrDefault(ItemKeys.MP_PCT,  PersistentDataType.DOUBLE,  0.0)
                pctAtk  += pdc.getOrDefault(ItemKeys.ATK_PCT, PersistentDataType.DOUBLE,  0.0)
                pctDef  += pdc.getOrDefault(ItemKeys.DEF_PCT, PersistentDataType.DOUBLE,  0.0)
                pctSpA  += pdc.getOrDefault(ItemKeys.SPA_PCT, PersistentDataType.DOUBLE,  0.0)
                pctSpD  += pdc.getOrDefault(ItemKeys.SPD_PCT, PersistentDataType.DOUBLE,  0.0)
            }

        s.finalHP      = (s.health  + flatHP)  * (1 + pctHP)
        s.finalMP      = (s.mp      + flatMP)  * (1 + pctMP)
        s.finalAttack  = (s.attack  + flatAtk) * (1 + pctAtk)
        s.finalDefense = (s.defense + flatDef) * (1 + pctDef)
        s.finalMagic   = (s.magic   + flatSpA) * (1 + pctSpA)
        s.finalMdef    = (s.mdef    + flatSpD) * (1 + pctSpD)

        SidebarService.updateNow(p)
    }

    private fun msg(t: String) = net.kyori.adventure.text.Component.text(t)
}

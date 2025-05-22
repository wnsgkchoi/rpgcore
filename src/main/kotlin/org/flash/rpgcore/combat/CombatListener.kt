package org.flash.rpgcore.combat

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.buff.BuffManager
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.classs.PlayerClass
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.stats.StatManager
import org.flash.rpgcore.util.WeaponRules // 변경된 WeaponRules 사용
import org.flash.rpgcore.equipment.EquipManager // EquipManager 추가 (장비된 무기 가져오기)
import org.flash.rpgcore.equipment.EquipmentDefs
import org.flash.rpgcore.equipment.ItemKeys
import org.bukkit.persistence.PersistentDataType
import org.flash.rpgcore.util.VirtualMp // VirtualMp 추가

class CombatListener : Listener {
    private val LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection()
    /** 근접 공격 계산 **/
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onMelee(e: EntityDamageByEntityEvent) {
        val attacker = e.damager as? Player ?: return
        val victim   = e.entity  as? LivingEntity ?: return
        val attackerClass = ClassManager.get(attacker.uniqueId)
        val itemInHand = attacker.inventory.itemInMainHand

        // 장비 GUI에서 현재 장착된 RPG 무기 가져오기
        val equippedRpgWeapon = EquipManager.getSlots(attacker.uniqueId).weapon

        // 1. 공격/스킬 액션 가능 여부 판단 (손에 든 아이템 Material + 장착된 RPG 무기 유효성)
        if (!WeaponRules.canPerformAction(attackerClass, itemInHand, equippedRpgWeapon)) {
            // 액션 불가 메시지는 canPerformAction 내부 또는 여기서 한 번만 보내는 것이 좋음
            // 예: attacker.sendActionBar(net.kyori.adventure.text.Component.text("§c지금은 공격할 수 없습니다."))
            // WeaponRules에서 false가 나오는 경우는 다양하므로, 상세 메시지 분기 필요시 WeaponRules 수정
            e.damage = 0.0
            e.isCancelled = true
            return
        }
        // 이 시점에서 equippedRpgWeapon은 null이 아니고 유효한 RPG 무기임이 보장됨 (canPerformAction 내부 로직에 의해)
        val rpgWeaponDef = EquipmentDefs.get(
            equippedRpgWeapon!!.itemMeta!!.persistentDataContainer.get(ItemKeys.EQUIP_ID, PersistentDataType.STRING)!!
        )!! // non-null 보장

        // 2. 일반 공격 쿨타임 검사 (RPG 무기의 Def에 cooldownMs가 설정된 경우)
        if (rpgWeaponDef.cooldownMs > 0) {
            if (WeaponAttackCooldownManager.isCooling(attacker)) {
                val remainingSec = WeaponAttackCooldownManager.getRemainingSeconds(attacker)
                attacker.sendActionBar(LEGACY_SERIALIZER.deserialize("§c공격 쿨타임: ${remainingSec}초 남음"))
                e.damage = 0.0
                e.isCancelled = true
                return
            }
        }

        // 3. 데미지 계산 (장착된 RPG 무기 기준)
        // itemInHand는 Material 검증용으로만 사용되었고, 실제 데미지 계산은 equippedRpgWeapon 기준
        e.damage = DamageCalculator.calc(attacker, equippedRpgWeapon, victim, 1.0, 0.0)

        // 4. 공격 성공 시 쿨타임 설정 (쿨타임이 있는 무기이고, 공격이 성공했을 때)
        if (rpgWeaponDef.cooldownMs > 0 && !e.isCancelled && e.damage > 0) {
            WeaponAttackCooldownManager.setCooldown(attacker, rpgWeaponDef)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerDamaged(e: EntityDamageEvent) {
        val victim = (e.entity as? Player) ?: return
        val cls    = ClassManager.get(victim.uniqueId)

        // 이미 처리된 무적시간이 있다면 추가 피해 방지 (VOID, SUICIDE 등 예외적 원인은 무시)
        if (victim.noDamageTicks > 0 && e.cause != EntityDamageEvent.DamageCause.VOID && e.cause != EntityDamageEvent.DamageCause.SUICIDE) {
            e.isCancelled = true
            return
        }

        // 1) 탱커 패시브: 모든 피해 50% 경감 + 반사
        if (cls == PlayerClass.TANK) {
            val origDamage  = e.damage
            val takenDamage = origDamage * 0.5 // 탱커는 받는 피해 50% 감소
            e.damage  = takenDamage

            // 반사 데미지 계산
            var reflectAmount = takenDamage * 0.05 + // 받은 피해의 5%
                    StatManager.load(victim.uniqueId).finalAttack * 0.15 // 자신의 공격력 15%

            // 꿰뚫는 가시 버프에 의한 추가 반사 데미지
            if (BuffManager.isActive(victim.uniqueId, "piercing_thorns")) {
                val coef = BuffManager.coef(victim.uniqueId, "piercing_thorns")
                val victimDefense  = StatManager.load(victim.uniqueId).finalDefense
                reflectAmount += victimDefense * coef
            }

            // 실제 반사 데미지 가하기 (가해자가 있고, 무적상태가 아닐때)
            if (e is EntityDamageByEntityEvent) {
                val directDamager = e.damager
                val actualAttacker: LivingEntity? = when (directDamager) {
                    is LivingEntity -> directDamager
                    is Projectile -> directDamager.shooter as? LivingEntity
                    else -> null
                }

                actualAttacker?.let { attackerEntity ->
                    if (attackerEntity.uniqueId != victim.uniqueId && attackerEntity.noDamageTicks == 0) { // 자기 자신에게 반사 X, 대상 무적 아닐 때
                        // 반사 데미지는 최소 1 보장
                        val finalReflectDamage = kotlin.math.max(1.0, reflectAmount)
                        // victim이 가해자(source)가 되어 데미지를 줌
                        attackerEntity.damage(finalReflectDamage, victim)
                    }
                }
            }

            victim.world.playSound(victim.location, org.bukkit.Sound.ENTITY_PLAYER_HURT, 1f, 1f)
            SidebarService.updateNow(victim)
            // 탱커 패시브 처리 후에는 다른 클래스 패시브나 일반 무적시간 설정을 건너뛰도록 return
            // UniversalInvincibilityListener 가 이후에 무적시간을 설정할 것임
            return
        }

        // 2) 원소술사 패시브: MP 흡수
        if (cls == PlayerClass.ELEMENTIST) {
            val incomingDamage = e.damage
            val currentMp = VirtualMp.get(victim)

            // MP로 흡수할 데미지량 (최대 현재 MP까지, 피해량의 60%)
            val mpToAbsorbDamage = (incomingDamage * 0.6).toInt()
            val actualMpUsed = kotlin.math.min(currentMp, mpToAbsorbDamage)

            if (actualMpUsed > 0) {
                VirtualMp.subtract(victim, actualMpUsed)
            }
            // MP로 흡수한 만큼 실제 받는 데미지 감소
            e.damage = kotlin.math.max(0.0, incomingDamage - actualMpUsed)


            SidebarService.updateNow(victim)
            // 원소술사 패시브 처리 후 return
            // UniversalInvincibilityListener 가 이후에 무적시간을 설정할 것임
            return
        }

        // 그 외 플레이어는 UniversalInvincibilityListener 에 의해 무적시간이 설정될 것임
    }

    /** Non-player entity 에게만 무적시간 해제 **/
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onAnyEntityDamageClearInvuln(e: EntityDamageEvent) {
        val victim = e.entity as? LivingEntity ?: return
        if (victim is Player) return

        // 몬스터는 피격 후 1틱 뒤에 무적시간 해제 (기존 로직 유지)
        Bukkit.getScheduler().runTaskLater(RPGCore.INSTANCE, Runnable {
            if (!victim.isDead && victim.isValid) { // isValid 추가: 이미 제거된 엔티티에 접근 방지
                victim.noDamageTicks = 0
            }
        }, 1L)
    }
}
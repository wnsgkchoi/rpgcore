package org.flash.rpgcore.skills

import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack // ItemStack 임포트
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import org.flash.rpgcore.RPGCore
import org.flash.rpgcore.buff.BuffManager
import org.flash.rpgcore.combat.DamageCalculator // DamageCalculator 임포트
// StatManager 등은 DamageCalculator 내부에서 사용하므로 여기서는 직접 필요 없을 수 있음
import org.flash.rpgcore.sidebar.SidebarService
import org.flash.rpgcore.stats.StatManager // lastStand에서 사용
import org.flash.rpgcore.util.VirtualHp // lastStand에서 사용
import kotlin.math.min
import kotlin.math.sqrt // lastStand에서 사용 (필요시)

object SkillEffects {

    /**
     * 스킬 ID에 따라 적절한 스킬 효과를 실행하는 dispatcher 함수.
     * @param p 시전자
     * @param skillDef 사용된 스킬의 SkillDefinition
     * @param skillLevel 사용된 스킬의 현재 레벨
     * @param equippedWeapon 시전자가 장비 GUI에 장착한 RPG 무기
     */
    fun executeEffect(p: Player, skillDef: SkillDefinition, skillLevel: Int, equippedWeapon: ItemStack) {
        // 스킬 데미지 계산에 사용될 스킬 자체의 계수
        val physCoef = skillDef.getAtkCoef(skillLevel)
        val magCoef  = skillDef.getMagCoef(skillLevel)
        val cooldownOrDuration = skillDef.getCooldown(skillLevel) // 스킬에 따라 쿨타임 또는 지속시간으로 사용

        when (skillDef.id) {
            "rage_whirlwind" -> rageWhirlwind(p, equippedWeapon, physCoef, magCoef)
            "crush_blow"     -> crushBlow(p, equippedWeapon, physCoef, magCoef)
            "last_stand"     -> lastStand(p, equippedWeapon, physCoef, magCoef) // last_stand는 특수 로직, 파라미터 조정 가능
            "shield_charge"  -> shieldCharge(p, equippedWeapon, physCoef, magCoef)
            "taunt"          -> taunt(p) // 타겟팅, 데미지 계산 없음
            "piercing_thorns"-> piercingThorns(p, physCoef, magCoef, cooldownOrDuration.toInt()) // 이 스킬은 버프 계열, phys/magCoef가 반사율 등에 사용될 수 있음
            else             -> p.sendActionBar(net.kyori.adventure.text.Component.text("§c지원되지 않는 스킬입니다: ${skillDef.id}"))
        }
    }

    /* 안전 정규화: 길이 0이면 null */
    private fun Vector.safeNormalize(): Vector? =
        if (lengthSquared() == 0.0) null else clone().normalize()

    /* 넉백 최대 값 */
    private const val KB_MAX = 1.1


    /** 1. 광전사 전용 스킬 **/

    /**
     * 분노의 광휘: 제자리에서 회전하면서 반경 5칸 내 모든 몬스터에게 피해 + 넉백
     * @param p 시전자
     * @param equippedWeapon 장착된 RPG 무기 (데미지 계산 기반)
     * @param skillPhysCoef 스킬의 물리 계수
     * @param skillMagCoef 스킬의 마법 계수
     */
    fun rageWhirlwind(p: Player, equippedWeapon: ItemStack, skillPhysCoef: Double, skillMagCoef: Double) {
        val center = p.location
        // StatManager는 DamageCalculator 내부에서 호출됨.
        // finalAttack, finalMagic 등은 이미 장비 스탯이 포함된 값.

        p.world.getNearbyEntities(center, 5.0, 3.0, 5.0)
            .filterIsInstance<LivingEntity>()
            .filter { it != p && it is Monster } // Monster 타입 체크
            .forEach { target ->
                // 1) 피해: DamageCalculator.calc에 장착된 무기와 스킬 계수 전달
                val dmg = DamageCalculator.calc(p, equippedWeapon, target, skillPhysCoef, skillMagCoef)
                target.damage(dmg, p)

                // 2) 넉백 (기존 로직 유지)
                val dir = target.location.toVector()
                    .subtract(center.toVector())
                    .safeNormalize() ?: return@forEach
                val kb  = dir.multiply(min(KB_MAX, 1.0)).setY(0.35)
                if (kb.isFinite()) target.velocity = kb
            }

        // 이펙트 (기존 로직 유지)
        p.world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f)
        p.world.spawnParticle(Particle.SWEEP_ATTACK, center, 20, 1.0, 0.5, 1.0, 0.1)
    }

    /**
     * 분쇄 강타: 큰 점프 후 착지 시 반경 6칸 내 몬스터에게 피해 + 넉백
     */
    fun crushBlow(p: Player, equippedWeapon: ItemStack, skillPhysCoef: Double, skillMagCoef: Double) {
        p.velocity = p.location.direction.setY(1.0).multiply(1.0) // 점프

        object : BukkitRunnable() {
            override fun run() {
                if (!p.isOnGround || !p.isValid) { // 플레이어가 유효한지 추가 확인
                    cancel()
                    return
                }
                // isOnGround만으로 착지 판정이 불안정할 수 있으므로, 몇 틱 더 지켜보거나,
                // 낙하 속도 변화 등을 감지하는 것이 더 정확할 수 있음. 여기서는 기존 방식 유지.
                if (!p.isOnGround) return // 아직 공중이면 대기
                cancel() // 착지했으므로 타이머 종료

                val center = p.location
                val world  = center.world

                world.spawnParticle(Particle.EXPLOSION, center, 1, 0.0, 0.0, 0.0, 0.0) // 착지 이펙트

                val radius = 6.0
                world.getNearbyEntities(center, radius, radius, radius)
                    .filterIsInstance<LivingEntity>()
                    .filter { it != p && it is Monster } // Monster 타입 체크, 플레이어 제외
                    .forEach { target ->
                        val dmg = DamageCalculator.calc(p, equippedWeapon, target, skillPhysCoef, skillMagCoef)
                        target.damage(dmg, p)

                        val kb = target.location.toVector()
                            .subtract(center.toVector())
                            .normalize()
                            .multiply(1.0) // 넉백 강도
                            .setY(0.5)     // 수직 넉백
                        if (kb.isFinite()) target.velocity = kb
                    }
            }
        }.runTaskTimer(RPGCore.INSTANCE, 0L, 1L) // 0틱 딜레이, 1틱 간격으로 실행 (착지 감지 주기)
    }

    /**
     * 결사의 일격: 체력 10%만 남기고 남은 HP를 절약해둔 뒤
     * 5초 내 다음 일반 공격(또는 특정 조건 만족 시)에 추가 피해 부여
     * (주의: 이 스킬은 다음 "일반 공격"에 효과를 부여하는 방식이므로, CombatListener와 연계 필요 가능성 있음)
     * 여기서는 즉시 발동형으로 가정하고, equippedWeapon 기반으로 데미지를 계산하는 것으로 수정.
     * 또는 "결사의 일격 버프"를 부여하고, CombatListener에서 해당 버프가 있을 때 데미지를 증폭시키는 방식도 가능.
     * 현재 코드는 즉시 발동형 + 조준형 공격으로 해석.
     */
    fun lastStand(p: Player, equippedWeapon: ItemStack, skillPhysCoef: Double, skillMagCoef: Double) {
        val stats = StatManager.load(p.uniqueId)
        val keepHpRatio = 0.10 // 남길 체력 비율 (10%)
        val minHpToKeep = 1.0  // 최소한 남길 체력 (절대값)
        val currentTotalHp = p.health + VirtualHp.get(p)
        val hpToKeep = kotlin.math.max(minHpToKeep, stats.finalHP * keepHpRatio)

        if (currentTotalHp <= hpToKeep) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("§cHP가 부족하여 결사의 일격을 사용할 수 없습니다!"))
            return
        }

        val hpLostForSkill = currentTotalHp - hpToKeep // 스킬 사용으로 잃게 될 (그리고 데미지로 전환될) HP

        // 실제 체력과 가상 체력에서 HP 차감
        var remainingLoss = hpLostForSkill
        val virtualHpToLose = kotlin.math.min(VirtualHp.get(p).toDouble(), remainingLoss)
        VirtualHp.set(p, (VirtualHp.get(p) - virtualHpToLose).toInt())
        remainingLoss -= virtualHpToLose

        if (remainingLoss > 0) {
            p.health = kotlin.math.max(1.0, p.health - remainingLoss) // 최소 1의 체력은 남도록 (바닐라 죽음 방지)
        }
        SidebarService.updateNow(p)

        p.sendActionBar(net.kyori.adventure.text.Component.text("§4결사의 일격 준비! (5초 내 공격)"))

        // 5초 타이머 (기존 로직과 유사)
        object : BukkitRunnable() {
            var ticksRemaining = 100 // 5초 (20 ticks/sec * 5 sec)
            override fun run() {
                ticksRemaining--
                if (ticksRemaining <= 0 || !p.isValid || p.isDead) {
                    p.sendActionBar(net.kyori.adventure.text.Component.text("§c결사의 일격 기회를 놓쳤습니다."))
                    cancel()
                    return
                }

                // 공격 대상 탐색 (기존 로직과 유사)
                // 여기서는 첫 번째 유효 타겟에게 즉시 데미지를 주는 것으로 가정
                // (만약 다음 "일반 공격"에 효과를 싣는 방식이면, 버프 매니저 사용 필요)
                val eyeLoc = p.eyeLocation
                val direction = eyeLoc.direction.normalize()
                val targetEntity = eyeLoc.world.rayTraceEntities(
                    eyeLoc, direction, 3.0, 0.5 // 탐색 거리 및 반경
                ) { entity -> entity is LivingEntity && entity != p && entity is Monster && entity.location.distanceSquared(eyeLoc) <= 9.0 }
                    ?.hitEntity as? LivingEntity

                if (targetEntity != null) {
                    // 기본 스킬 데미지 (계수 기반)
                    val baseSkillDamage = DamageCalculator.calc(p, equippedWeapon, targetEntity, skillPhysCoef, skillMagCoef)
                    // 잃은 체력 기반 추가 데미지 (예: 잃은 체력의 30%)
                    val extraDamageFromHp = hpLostForSkill * 0.30
                    val finalTotalDamage = baseSkillDamage + extraDamageFromHp

                    targetEntity.damage(finalTotalDamage, p)
                    targetEntity.world.spawnParticle(Particle.CRIT, targetEntity.location.add(0.0, targetEntity.height / 2, 0.0), 30, 0.4, 0.5, 0.4, 0.2)

                    val kbDir = direction.clone().multiply(1.2).setY(0.4) // 넉백 방향 및 강도
                    if (kbDir.isFinite()) targetEntity.velocity = kbDir

                    p.sendActionBar(net.kyori.adventure.text.Component.text("§c결사의 일격 명중!"))
                    cancel() // 스킬 성공 시 타이머 종료
                }
            }
        }.runTaskTimer(RPGCore.INSTANCE, 0L, 1L) // 즉시 시작, 매 틱 실행
    }


    /** 2. 탱커 전용 스킬 **/
    fun shieldCharge(p: Player, equippedWeapon: ItemStack, skillPhysCoef: Double, skillMagCoef: Double) {
        // 기존 로직에서 weapon: Material 대신 equippedWeapon: ItemStack을 DamageCalculator.calc에 전달
        val world  = p.world
        val start  = p.location
        val dir    = start.direction.clone().setY(0.0).normalize() // y축 이동은 없도록
        val MAX_DIST = 5.0 // 최대 돌진 거리
        val ENTITY_TRACE_RADIUS = 0.8 // 엔티티 충돌 감지 반경

        // 가장 가까운 블록 충돌 지점 계산
        val blockHitResult = world.rayTraceBlocks(start.clone().add(0.0,0.5,0.0), dir, MAX_DIST) // 시야 약간 위에서 발사
        val distanceToBlock = blockHitResult?.hitPosition?.distance(start.toVector()) ?: MAX_DIST

        // 가장 가까운 엔티티 충돌 지점 계산 (몬스터만)
        val entityHitResult = world.rayTraceEntities(
            start.clone().add(0.0,0.5,0.0), // 시야 약간 위에서 발사
            dir,
            MAX_DIST,
            ENTITY_TRACE_RADIUS
        ) { entity -> entity is Monster && entity != p }
        val distanceToEntity = entityHitResult?.hitPosition?.distance(start.toVector()) ?: MAX_DIST
        val hitEntity = entityHitResult?.hitEntity as? Monster

        // 실제 돌진 거리 결정 (더 짧은 쪽으로)
        val actualDashDistance = kotlin.math.min(kotlin.math.min(distanceToBlock, distanceToEntity), MAX_DIST)
        val destination = start.clone().add(dir.multiply(actualDashDistance))

        p.teleport(destination) // 목적지로 텔레포트
        p.fallDistance = 0f // 낙하 데미지 방지
        // p.noDamageTicks = p.maximumNoDamageTicks // 돌진 중 짧은 무적 (선택 사항)

        // 충돌한 주 대상에게 데미지 및 넉백
        hitEntity?.let { mainTarget ->
            if (mainTarget.location.distanceSquared(destination) < (ENTITY_TRACE_RADIUS * 2) * (ENTITY_TRACE_RADIUS * 2)) { // 유효 범위 내 충돌 확인
                val dmg = DamageCalculator.calc(p, equippedWeapon, mainTarget, skillPhysCoef, skillMagCoef)
                mainTarget.damage(dmg, p)
                val kb = dir.clone().multiply(1.5).setY(0.3) // 주 대상 넉백 강도
                if(kb.isFinite()) mainTarget.velocity = kb
            }
        }

        // 돌진 도착 지점 주변 범위 피해 (주 대상 제외)
        val aoeRadius = 3.0 // 주변 피해 반경
        world.getNearbyEntities(destination, aoeRadius, aoeRadius, aoeRadius)
            .filterIsInstance<LivingEntity>()
            .filter { it != p && it != hitEntity && it is Monster }
            .forEach { victim ->
                val dmg = DamageCalculator.calc(p, equippedWeapon, victim, skillPhysCoef * 0.5, skillMagCoef * 0.5) // 주변은 50% 데미지
                victim.damage(dmg, p)
            }

        world.spawnParticle(Particle.SWEEP_ATTACK, destination, 30, 1.0, 0.1, 1.0, 0.1)
        world.playSound(destination, Sound.ITEM_SHIELD_BLOCK, 1.2f, 0.8f)
        world.playSound(destination, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f)
    }

    fun taunt(p: Player) {
        // 기존 로직 유지 (데미지 계산 없음)
        val range = 10.0
        val durationTicks = 80L // 4초
        val nearbyMonsters = p.world.getNearbyEntities(p.location, range, range, range)
            .filterIsInstance<Monster>()

        if (nearbyMonsters.isEmpty()) {
            p.sendActionBar(net.kyori.adventure.text.Component.text("§7주변에 몬스터가 없습니다."))
            return
        }

        nearbyMonsters.forEach { monster ->
            monster.target = p
        }

        p.sendActionBar(net.kyori.adventure.text.Component.text("§e몬스터를 도발했습니다! (${durationTicks / 20}초)"))
        p.world.spawnParticle(Particle.ANGRY_VILLAGER, p.location.add(0.0, 1.0, 0.0), 20, 0.5, 0.5, 0.5, 0.1)
        p.world.playSound(p.location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.5f)

        // 도발 지속시간 동안 주기적으로 타겟 재설정 (몬스터가 다른 것에 어그로 끌리는 것 방지)
        // BukkitRunnable을 사용할 수도 있지만, 여기서는 한 번만 설정. 필요시 BuffManager 연동 가능.
    }

    /**
     * 꿰뚫는 가시: 방어력 비례 반사 버프
     * @param p 시전자
     * @param buffCoef 스킬 정의에 따른 계수 (예: 방어력의 몇 %를 반사 데미지로 추가할지)
     * @param buffMagCoef 여기서는 사용되지 않지만, 마법 반사 등 확장 시 사용 가능
     * @param durationSec 버프 지속 시간(초)
     */
    fun piercingThorns(p: Player, buffCoef: Double, buffMagCoef: Double, durationSec: Int) {
        // 기존 로직에서 BuffManager.activate 호출 부분 유지
        val buffId = "piercing_thorns"
        if (BuffManager.isActive(p.uniqueId, buffId)) {
            // 이미 활성화된 경우, 지속시간을 갱신하거나 중첩 불가 메시지 표시
            // 여기서는 지속시간 갱신으로 가정
            BuffManager.activate(p.uniqueId, buffId, buffCoef, durationSec) // buffCoef는 반사 데미지 계산 시 활용될 계수
            p.sendActionBar(net.kyori.adventure.text.Component.text("§a꿰뚫는 가시 효과가 갱신되었습니다! (${durationSec}s)"))
        } else {
            BuffManager.activate(p.uniqueId, buffId, buffCoef, durationSec)
            p.sendActionBar(net.kyori.adventure.text.Component.text("§a꿰뚫는 가시 발동! (${durationSec}s)"))
        }
        p.world.playSound(p.location, Sound.ITEM_SHIELD_BLOCK, 1f, 1.2f)
        // 이펙트 (예: 플레이어 주변에 가시 파티클)
        object : BukkitRunnable() {
            var remainingTicks = durationSec * 20
            override fun run() {
                remainingTicks--
                if (remainingTicks <= 0 || !p.isValid || !BuffManager.isActive(p.uniqueId, buffId)) {
                    cancel()
                    return
                }
                if (remainingTicks % 10 == 0) { // 0.5초마다 파티클
                    p.world.spawnParticle(Particle.CRIT, p.location.add(0.0, 1.0, 0.0), 5, 0.3, 0.3, 0.3, 0.05)
                }
            }
        }.runTaskTimer(RPGCore.INSTANCE, 0L, 1L)
    }

    /* Vector 가 유한한 값인지 검사 */
    private fun Vector.isFinite(): Boolean =
        !(x.isNaN() || y.isNaN() || z.isNaN()
                || x.isInfinite() || y.isInfinite() || z.isInfinite())
}
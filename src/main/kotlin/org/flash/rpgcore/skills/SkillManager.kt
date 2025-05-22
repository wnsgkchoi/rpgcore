// File: SkillManager.kt
package org.flash.rpgcore.skills

import org.bukkit.Bukkit // Bukkit API 추가 (플레이어 메시지 전송용)
import org.flash.rpgcore.classs.ClassManager // ClassManager 추가
import org.flash.rpgcore.classs.PlayerClass // PlayerClass 추가
import java.util.*

data class PlayerSkills(
    val learned: MutableMap<String, Int> = mutableMapOf(), // 학습한 스킬 ID -> 레벨
    var slot1: Pair<String, Int>? = null, // 슬롯 1: 스킬 ID, 현재 레벨
    var slot2: Pair<String, Int>? = null, // 슬롯 2: 스킬 ID, 현재 레벨
    var slot3: Pair<String, Int>? = null  // 슬롯 3: 스킬 ID, 현재 레벨
) {
    fun get(slot: Int): Pair<String, Int>? = when (slot) {
        1 -> slot1
        2 -> slot2
        3 -> slot3
        else -> null
    }

    fun set(slot: Int, value: Pair<String, Int>?) {
        when (slot) {
            1 -> slot1 = value
            2 -> slot2 = value
            3 -> slot3 = value
        }
    }
}

object SkillManager {
    private val cache = mutableMapOf<UUID, PlayerSkills>()

    fun getSkills(uuid: UUID): PlayerSkills {
        // NOVICE 클래스는 스킬 정보를 가지지 않음 (항상 빈 PlayerSkills 반환)
        if (ClassManager.get(uuid) == PlayerClass.NOVICE) {
            return PlayerSkills()
        }

        return cache.computeIfAbsent(uuid) {
            val ps = PlayerSkills()
            // 1) 학습된 레벨 로드
            ps.learned.putAll(SkillStore.loadLearnedLevels(uuid))
            // 2) 슬롯(1~3) 로드
            for (s in 1..3) {
                SkillStore.loadSkillId(uuid, s)?.let { id ->
                    val lvl = SkillStore.loadSkillLevel(uuid, s)
                    ps.set(s, id to lvl)
                    // 슬롯 레벨이 learned보다 크면 learned에 반영
                    // (또는 learned 레벨이 슬롯 레벨보다 항상 크거나 같아야 함 - 정책 필요)
                    val prevLearnedLevel = ps.learned[id] ?: 0
                    if (lvl > prevLearnedLevel) {
                        // 일반적으로 장착된 스킬 레벨이 학습된 레벨보다 높을 수는 없음.
                        // 만약 이런 경우가 발생한다면, 학습된 레벨을 장착 레벨로 업데이트 하거나,
                        // 장착 레벨을 학습된 레벨로 낮추어야 함.
                        // 여기서는 학습된 레벨을 우선시하여, 장착 레벨이 더 높다면 학습 레벨을 올려줌.
                        // (또는 SkillStore.loadSkillLevel이 항상 learned 레벨 이하를 반환하도록 보장해야 함)
                        ps.learned[id] = lvl
                    } else if (lvl < prevLearnedLevel && lvl > 0) {
                        // 만약 장착된 스킬의 레벨이 학습된 레벨보다 낮다면 (0이 아닌데),
                        // 이는 약간 이상한 상태일 수 있음. 보통은 학습된 레벨로 장착됨.
                        // 여기서는 SkillStore에서 로드한 레벨을 그대로 사용.
                    }
                }
            }
            // 3) 로드 과정에서 learned 맵이 변경되었을 수 있으므로, 최종 learned 상태를 저장.
            // (예: 슬롯에만 있던 스킬 정보가 learned로 동기화된 경우)
            // 단, 이 로직은 getSkills 호출 시마다 발생하므로, 변경이 있을 때만 저장하는 것이 효율적.
            // 현재는 항상 저장.
            SkillStore.saveLearnedLevels(uuid, ps.learned)
            ps
        }
    }

    fun learnSkill(uuid: UUID, skillId: String, level: Int = 1) {
        // NOVICE는 스킬을 학습할 수 없음
        if (ClassManager.get(uuid) == PlayerClass.NOVICE) {
            Bukkit.getPlayer(uuid)?.sendMessage("§cNOVICE는 스킬을 배울 수 없습니다.")
            return
        }

        val ps = getSkills(uuid) // 캐시 또는 파일에서 로드 (NOVICE가 아니므로 정상 로드)
        val currentLearnedLevel = ps.learned[skillId] ?: 0

        // 더 높은 레벨로 학습하거나, 새로 학습하는 경우에만 처리
        if (level > currentLearnedLevel) {
            ps.learned[skillId] = level
            SkillStore.saveLearnedLevels(uuid, ps.learned) // 변경된 학습 정보 저장
            // 필요시 플레이어에게 알림
            Bukkit.getPlayer(uuid)?.sendMessage("§a스킬 [${SkillDefs.get(skillId)?.displayName ?: skillId}] §f(Lv.$level)§a 학습 완료!")
        } else if (level <= 0){
            // 레벨이 0 이하로 주어지면 스킬 망각으로 처리할 수도 있음 (현재는 무시)
            Bukkit.getPlayer(uuid)?.sendMessage("§c유효하지 않은 스킬 레벨입니다: $level")
        } else {
            Bukkit.getPlayer(uuid)?.sendMessage("§e이미 해당 스킬을 더 높은 레벨로 알고 있거나 같은 레벨입니다.")
        }
    }

    fun updateSkill(uuid: UUID, slot: Int, skillId: String?, level: Int) {
        // NOVICE는 스킬을 장착할 수 없음
        if (ClassManager.get(uuid) == PlayerClass.NOVICE) {
            Bukkit.getPlayer(uuid)?.sendMessage("§cNOVICE는 스킬을 장착할 수 없습니다.")
            // NOVICE의 경우, 해당 슬롯을 비우도록 SkillStore에 명시적으로 저장할 수 있음
            // SkillStore.saveSkill(uuid, slot, null, 0) // 슬롯 비우기
            // cache에서도 해당 플레이어의 슬롯을 비워야 함 (getSkills가 항상 새 PlayerSkills를 반환하므로 자동 처리될 수 있음)
            val ps = cache[uuid] // 직접 캐시 접근은 지양하는 것이 좋으나, 여기서는 NOVICE의 슬롯을 명시적으로 비우기 위함.
            ps?.set(slot, null)  // 캐시에서도 비움 (만약 이전에 다른 클래스였다가 NOVICE로 변경된 직후라면)
            SkillStore.saveSkill(uuid, slot, null, 0) // 파일에서도 확실히 비움
            return
        }

        val ps = getSkills(uuid) // 캐시 또는 파일에서 로드

        if (skillId == null || level <= 0) { // 스킬 해제 또는 유효하지 않은 레벨
            ps.set(slot, null)
            SkillStore.saveSkill(uuid, slot, null, 0)
            Bukkit.getPlayer(uuid)?.sendMessage("§e${slot}번 스킬 슬롯이 비워졌습니다.")
            return
        }

        // 장착하려는 스킬을 학습했는지, 그리고 요청된 레벨이 학습된 레벨 이하인지 확인
        val learnedLevel = ps.learned[skillId]
        if (learnedLevel == null) {
            Bukkit.getPlayer(uuid)?.sendMessage("§c먼저 스킬 [$skillId]을(를) 배워야 합니다.")
            return
        }
        if (level > learnedLevel) {
            Bukkit.getPlayer(uuid)?.sendMessage("§c스킬 [$skillId]의 장착 레벨($level)이 학습한 레벨($learnedLevel)보다 높을 수 없습니다.")
            return
        }

        // 유효한 경우 슬롯 업데이트 및 저장
        ps.set(slot, skillId to level)
        SkillStore.saveSkill(uuid, slot, skillId, level)
        Bukkit.getPlayer(uuid)?.sendMessage("§a${slot}번 슬롯에 스킬 [${SkillDefs.get(skillId)?.displayName ?: skillId}] §f(Lv.$level)§a 장착 완료!")

        // 장착된 스킬 레벨이 학습된 레벨보다 항상 낮거나 같도록 보장되었으므로,
        // learned 맵을 여기서 다시 동기화할 필요는 없음. (learnSkill에서만 learned 갱신)
    }

    fun clear(uuid: UUID) {
        cache.remove(uuid)
        // 파일에서 스킬 정보를 삭제하는 로직은 별도로 필요 (예: 클래스 초기화 시)
    }

    fun reloadAllPlayers() {
        // 현재 온라인 중인 모든 플레이어의 스킬 정보 다시 로드
        val onlinePlayers = Bukkit.getOnlinePlayers()
        val currentCacheKeys = HashSet(cache.keys)

        onlinePlayers.forEach { player ->
            cache.remove(player.uniqueId) // 기존 캐시 제거
            getSkills(player.uniqueId)    // 다시 로드 (NOVICE면 빈 객체)
            currentCacheKeys.remove(player.uniqueId)
        }

        // 오프라인 플레이어 중 캐시에 남아있던 정보 제거 (선택적)
        currentCacheKeys.forEach { uuid ->
            cache.remove(uuid)
        }
        // GUI 재오픈 로직 등은 SkillEquipGUI 등에서 필요시 처리
    }
}
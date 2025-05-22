package org.flash.rpgcore

import org.bukkit.Bukkit
import org.flash.rpgcore.equipment.EquipGUI
import org.flash.rpgcore.stats.StatGUI
import org.flash.rpgcore.equipment.EquipStore
import org.flash.rpgcore.equipment.EquipListener
import org.flash.rpgcore.stats.StatListener
import org.flash.rpgcore.sidebar.SidebarService
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.flash.rpgcore.buff.BuffManager
import org.flash.rpgcore.classs.ClassGUI
import org.flash.rpgcore.classs.ClassManager
import org.flash.rpgcore.classs.PlayerClass
import org.flash.rpgcore.combat.CombatListener
import org.flash.rpgcore.command.GiveEquipCommand
import org.flash.rpgcore.command.PracticeCommand
import org.flash.rpgcore.command.ReloadCommand
import org.flash.rpgcore.command.TradeCommand
import org.flash.rpgcore.equipment.EquipmentDefs
import org.flash.rpgcore.equipment.RecipeDefs
import org.flash.rpgcore.classs.ClassListener
import org.flash.rpgcore.listener.Fishing
import org.flash.rpgcore.listener.ForbiddenItemListener
import org.flash.rpgcore.combat.NaturalRegenBlocker
import org.flash.rpgcore.skills.SkillListener
import org.flash.rpgcore.skills.SkillUseListener
import org.flash.rpgcore.listener.ToolDurabilityListener
import org.flash.rpgcore.combat.UniversalInvincibilityListener
import org.flash.rpgcore.combat.VirtualHealthDeathListener
import org.flash.rpgcore.combat.VirtualHealthListener
import org.flash.rpgcore.command.GiveSkillCommand
import org.flash.rpgcore.command.ListSkillsCommand
import org.flash.rpgcore.task.MpRegenTask
import org.flash.rpgcore.skills.SkillCommand
import org.flash.rpgcore.skills.SkillDefs
import org.flash.rpgcore.skills.SkillManager
import org.flash.rpgcore.task.HealthRegenTask
import org.flash.rpgcore.trade.TradeListener
import org.flash.rpgcore.util.VirtualHp
import org.flash.rpgcore.util.VirtualMp
import java.io.File

class RPGCore : JavaPlugin() {
    companion object {
        lateinit var INSTANCE: RPGCore
            private set
    }

    lateinit var equipStore: EquipStore
        private set

    override fun onEnable() {
        INSTANCE = this

        // 설정 파일 로드
        saveDefaultConfig()
        EquipmentDefs.reload()
        RecipeDefs.load()
        SkillDefs.reload()

        // 스토어 및 서비스/매니저 초기화
        equipStore = EquipStore(this)

        // 이벤트 리스너 등록
        val pm = server.pluginManager
        pm.registerEvents(EquipListener(), this)
        pm.registerEvents(StatListener(), this)
        pm.registerEvents(ClassListener(), this)
        pm.registerEvents(CombatListener(), this)
        pm.registerEvents(VirtualHealthListener(), this)
        pm.registerEvents(NaturalRegenBlocker(), this)
        pm.registerEvents(SkillListener(), this)
        pm.registerEvents(ForbiddenItemListener(), this)
        pm.registerEvents(VirtualHealthDeathListener(), this)
        pm.registerEvents(UniversalInvincibilityListener(), this)
        pm.registerEvents(SkillUseListener(), this)
        pm.registerEvents(Fishing(), this)
        pm.registerEvents(ToolDurabilityListener(), this)
        pm.registerEvents(TradeListener(), this)

        // 사이드바 및 회복 태스크 시작
        SidebarService.init(this)
        MpRegenTask.start(this)
        HealthRegenTask.start(this)
        BuffManager.startTask(this)
        
        // 명령어 로드
        getCommand("trade")?.setExecutor(TradeCommand())
        getCommand("rpgcore")?.setExecutor(ReloadCommand())
        getCommand("practice")!!.setExecutor(PracticeCommand())
        getCommand("giveequip")?.setExecutor(GiveEquipCommand())
        getCommand("skill")?.setExecutor(SkillCommand())
        getCommand("giveskill")?.setExecutor(GiveSkillCommand())
        getCommand("listskills")?.setExecutor(ListSkillsCommand())

        logger.info("RPGCore has been enabled")
    }

    override fun onDisable() {
        // 별도 stop 메서드 없음 — 스케줄러는 서버 종료 시 자동 해제됩니다.
    }

    override fun onCommand(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) return false
        when (cmd.name.lowercase()) {
            "equip" -> EquipGUI.open(sender)
            "stat"  -> StatGUI.open(sender)
            "class" -> ClassGUI.open(sender)
            "fillmax" -> {
                if (!sender.hasPermission("rpgcore.op")) return false
                if (args.isEmpty()) {
                    sender.sendMessage("§c사용법: /classreset <플레이어이름>")
                    return true
                }
                val target = server.getPlayerExact(args[0])
                if (target == null) {
                    sender.sendMessage("§c플레이어 '${args[0]}' 를 찾을 수 없습니다.")
                    return true
                }
                VirtualHp.fillMax(target)
                VirtualMp.fillMax(target)
                return true
            }
            "classreset" -> {
                if (!sender.hasPermission("rpgcore.op")) return false
                if (args.isEmpty()) {
                    sender.sendMessage("§c사용법: /classreset <플레이어이름>")
                    return true
                }
                val target = server.getPlayerExact(args[0])
                if (target == null) {
                    sender.sendMessage("§c플레이어 '${args[0]}' 를 찾을 수 없습니다.")
                    return true
                }

                // 1) 클래스 초기화
                ClassManager.set(target.uniqueId, PlayerClass.NOVICE)

                // 3) 장비 초기화
                EquipStore.saveItem(target.uniqueId, "weapon", null)
                // (필요하다면 다른 파츠도 모두 null 처리)

                // 4) 스킬 저장 파일 삭제 → 모든 슬롯이 비워집니다
                val skillsDir = File(RPGCore.INSTANCE.dataFolder, "skills")
                val skillFile = File(skillsDir, "${target.uniqueId}.yml")
                if (skillFile.exists()) {
                    val cfg = YamlConfiguration.loadConfiguration(skillFile)
                    for (slot in 1..3) {
                        cfg.set("slot$slot.id", null)
                        cfg.set("slot$slot.level", null)
                    }
                    cfg.save(skillFile)
                }

                // 5) 캐시 초기화 & 재로드
                SkillManager.clear(target.uniqueId)
                SkillManager.getSkills(target.uniqueId) // 빈 상태로 재생성

                // 6) 사이드바 갱신
                SidebarService.updateNow(target)

                sender.sendMessage("§e${target.name}님의 클래스가 초보자로 초기화되었습니다.")
                target.sendMessage("§e당신의 클래스가 초보자로 초기화되었습니다.")
            }
            else -> return false
        }
        return true
    }
}


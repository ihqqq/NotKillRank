package me.ihqqq.notkillrank.listener;

import me.chancesd.pvpmanager.event.PlayerCombatLogEvent;
import me.chancesd.pvpmanager.event.PlayerTagEvent;
import me.chancesd.pvpmanager.event.PlayerUntagEvent;
import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.file.module.PvPManagerFile;
import me.ihqqq.notkillrank.hook.PvPManagerHook;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.manager.StreakManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

public class PvPManagerListener implements Listener {

    public PvPManagerListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTagged(PlayerTagEvent event) {
        if (!event.isAttacker() && event.getEnemy() != null) {
            PvPManagerHook.getInstance().recordAttacker(
                    event.getPlayer().getUniqueId(),
                    event.getEnemy().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUntagged(PlayerUntagEvent event) {
        PvPManagerHook.getInstance().clearPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = getPlayerAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;

        if (victim.hasMetadata("NPC") || attacker.hasMetadata("NPC")) return;

        if (!PvPManagerFile.get().getBoolean("block-attack-if-victim-protected", true)) return;
        if (!Settings.MODULE_PROTECTION) return;

        PlayerData victimData = PluginDataManager.getPlayerDatabase(victim.getUniqueId().toString());
        if (victimData == null) return;

        if (EloManager.getInstance().isNewbie(victimData)) {
            event.setCancelled(true);
            String msg = MessageUtil.getMessage("pvpmanager-protection-block",
                            "<red>⚔ <white>{victim} <gray>đang được bảo vệ người mới — không thể tấn công!")
                    .replace("{victim}", victim.getName());
            MessageUtil.sendMessage(attacker, msg);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatLog(PlayerCombatLogEvent event) {
        Player logger = event.getPlayer();
        UUID loggerUUID = logger.getUniqueId();

        UUID attackerUUID = PvPManagerHook.getInstance().popLastAttacker(loggerUUID);

        PlayerData loggerData = PluginDataManager.getPlayerDatabase(loggerUUID.toString());
        if (loggerData == null) return;

        var cfg = PvPManagerFile.get();
        double lossMultiplier = cfg.getDouble("combat-log.elo-loss-multiplier", 2.0);
        boolean attackerGains = cfg.getBoolean("combat-log.attacker-gains-elo", true);
        boolean broadcast     = cfg.getBoolean("combat-log.broadcast", true);

        int baseElo = (int) Math.floor(loggerData.getElo() * (Settings.ELO_KILL_PERCENT / 100.0));
        if (baseElo < 1) baseElo = 1;
        int eloLoss = (int) Math.round(baseElo * lossMultiplier);

        int oldLoggerElo = loggerData.getElo();
        int newLoggerElo = Math.max(Settings.ELO_MIN, oldLoggerElo - eloLoss);
        int actualLoss   = oldLoggerElo - newLoggerElo;

        loggerData.setElo(newLoggerElo);
        loggerData.setDeaths(loggerData.getDeaths() + 1);
        loggerData.setDeathStreak(loggerData.getDeathStreak() + 1);
        loggerData.setKillStreak(0);
        PluginDataManager.invalidateTopCache();

        applyAttackerReward(attackerUUID, actualLoss, attackerGains, broadcast, logger);

        final String loggerUuidStr = loggerUUID.toString();
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () ->
                PluginDataManager.savePlayerDatabaseToStorage(loggerUuidStr));
    }

    private void applyAttackerReward(UUID attackerUUID, int actualLoss,
                                     boolean attackerGains, boolean broadcast, Player logger) {
        Player attacker = (attackerUUID != null) ? Bukkit.getPlayer(attackerUUID) : null;
        PlayerData attackerData = null;
        int eloGain = 0;

        if (attacker != null && attacker.isOnline()) {
            attackerData = PluginDataManager.getOrCreate(attacker);

            attackerData.setKills(attackerData.getKills() + 1);
            attackerData.setKillStreak(attackerData.getKillStreak() + 1);
            if (attackerData.getKillStreak() > attackerData.getHighestKillStreak()) {
                attackerData.setHighestKillStreak(attackerData.getKillStreak());
            }
            attackerData.setDeathStreak(0);

            if (attackerGains && actualLoss > 0) {
                String loggerUuidStr = logger.getUniqueId().toString();
                boolean antiFarm = Settings.MODULE_ANTI_FARM
                        && EloManager.getInstance().isAntiFarm(attacker, attackerData, loggerUuidStr);
                if (antiFarm) {
                    int effectiveLimit = EloManager.getInstance().resolveAntiFarmLimit(attacker);
                    String antiMsg = MessageUtil.getMessage("anti-farm",
                                    "<gray>(Không nhận elo — Đã giết {victim} quá {limit} lần/giờ)")
                            .replace("{victim}", logger.getName())
                            .replace("{limit}", String.valueOf(effectiveLimit));
                    MessageUtil.sendMessage(attacker, antiMsg);
                } else {
                    eloGain = actualLoss;
                    int oldElo = attackerData.getElo();
                    int newElo = oldElo + eloGain;
                    attackerData.setElo(newElo);
                    if (newElo > attackerData.getPeakElo()) attackerData.setPeakElo(newElo);
                    RankManager.getInstance().checkRankUp(attacker, attackerData, oldElo, newElo);
                    if (Settings.MODULE_ANTI_FARM) {
                        EloManager.getInstance().logKill(attackerData, loggerUuidStr);
                    }
                }
            }

            if (Settings.MODULE_STREAKS) {
                StreakManager.getInstance().checkMilestone(attacker, attackerData);
            }
        }

        if (broadcast) broadcastCombatLog(logger, attacker, attackerData, actualLoss, eloGain, attackerGains);

        if (attacker != null && attacker.isOnline() && eloGain > 0) {
            String notify = MessageUtil.getMessage("pvpmanager-combat-log-notify",
                            "<green>+{elo_gain} elo <gray>— <white>{logger} <gray>đã thoát game trong combat!")
                    .replace("{elo_gain}", String.valueOf(eloGain))
                    .replace("{logger}", logger.getName());
            MessageUtil.sendMessage(attacker, notify);
        }

        if (attackerData != null && attacker != null) {
            final String attackerUuidStr = attacker.getUniqueId().toString();
            Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () ->
                    PluginDataManager.savePlayerDatabaseToStorage(attackerUuidStr));
        }
    }

    private void broadcastCombatLog(Player logger, Player attacker, PlayerData attackerData,
                                    int actualLoss, int eloGain, boolean attackerGains) {
        if (attacker != null && attacker.isOnline() && attackerGains && eloGain > 0) {
            String rankAttacker = RankManager.getInstance().getRankTag(attackerData.getElo() - eloGain);
            String msg = MessageUtil.getMessage("pvpmanager-combat-log-with-attacker",
                            "<red>☠ <white>{logger} <gray>thoát game trong combat! "
                                    + "<red>-{elo_loss} elo<gray>. "
                                    + "{rank_attacker}<white>{attacker} <gray>nhận <green>+{elo_gain} elo<gray>.")
                    .replace("{logger}", logger.getName())
                    .replace("{elo_loss}", String.valueOf(actualLoss))
                    .replace("{rank_attacker}", rankAttacker)
                    .replace("{attacker}", attacker.getName())
                    .replace("{elo_gain}", String.valueOf(eloGain));
            MessageUtil.sendBroadcast(msg);
        } else {
            String msg = MessageUtil.getMessage("pvpmanager-combat-log",
                            "<red>☠ <white>{logger} <gray>thoát game trong combat! <red>-{elo_loss} elo<gray>.")
                    .replace("{logger}", logger.getName())
                    .replace("{elo_loss}", String.valueOf(actualLoss));
            MessageUtil.sendBroadcast(msg);
        }
    }

    private Player getPlayerAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p) return p;
        if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player p) return p;
        return null;
    }
}

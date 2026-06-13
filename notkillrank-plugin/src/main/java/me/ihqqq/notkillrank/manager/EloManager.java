package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.file.module.EloFile;
import me.ihqqq.notkillrank.file.module.StreaksFile;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class EloManager {

    private static EloManager instance;

    public EloManager() {
        instance = this;
    }

    public static EloManager getInstance() {
        return instance;
    }


    public void processKill(Player killer, Player victim) {
        PlayerData killerData = PluginDataManager.getOrCreate(killer);
        PlayerData victimData = PluginDataManager.getOrCreate(victim);

        boolean killerNewbie = isNewbie(killerData);
        boolean victimNewbie = isNewbie(victimData);

        if (victimNewbie || killerNewbie) {
            applyKillStats(killerData, victimData);
            victimData.setLastKillerUUID(killer.getUniqueId().toString());
            victimData.setLastKilledTime(System.currentTimeMillis());
            victimData.setNoDeathStart(System.currentTimeMillis());

            String msg = MessageUtil.getMessage("kill-no-elo",
                            "<reset>{rank_killer} <white>{killer} <white>đã chọc chết "
                                    + "{rank_victim} <white>{victim} <gray>(bảo vệ người mới)")
                    .replace("{rank_killer}", RankManager.getInstance().getRankTag(killerData.getElo()))
                    .replace("{killer}", killer.getName())
                    .replace("{rank_victim}", RankManager.getInstance().getRankTag(victimData.getElo()))
                    .replace("{victim}", victim.getName());
            MessageUtil.sendBroadcast(msg);

            final String killerUuid = killer.getUniqueId().toString();
            final String victimUuid = victim.getUniqueId().toString();
            Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
                PluginDataManager.savePlayerDatabaseToStorage(killerUuid);
                PluginDataManager.savePlayerDatabaseToStorage(victimUuid);
            });
            return;
        }

        boolean antiFarmEnabled = Settings.MODULE_ANTI_FARM;
        if (antiFarmEnabled && isAntiFarm(killerData, victim.getUniqueId().toString())) {
            String anti = MessageUtil.getMessage("anti-farm",
                            "<gray>(Không nhận elo — Đã giết {victim} quá {limit} lần/giờ)")
                    .replace("{victim}", victim.getName())
                    .replace("{limit}", String.valueOf(Settings.ANTI_FARM_LIMIT_KILLS_PER_HOUR));
            MessageUtil.sendMessage(killer, anti);

            killerData.setKills(killerData.getKills() + 1);
            victimData.setDeaths(victimData.getDeaths() + 1);

            final String killerUuid = killer.getUniqueId().toString();
            final String victimUuid = victim.getUniqueId().toString();
            Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
                PluginDataManager.savePlayerDatabaseToStorage(killerUuid);
                PluginDataManager.savePlayerDatabaseToStorage(victimUuid);
            });
            return;
        }

        KillEloBreakdown breakdown = calculateBreakdown(killer, killerData, victimData);

        boolean streakModuleEnabled = Settings.MODULE_STREAKS;
        if (streakModuleEnabled && breakdown.isStreakBreak) {
            StreakManager.getInstance().broadcastStreakBreak(killer, killerData, victimData,
                    breakdown.eloGained);
        }

        int newKillerElo = killerData.getElo() + breakdown.eloGained;
        int newVictimElo = Math.max(Settings.ELO_MIN, victimData.getElo() - breakdown.totalVictimLoss);

        killerData.setElo(newKillerElo);
        if (newKillerElo > killerData.getPeakElo()) killerData.setPeakElo(newKillerElo);
        victimData.setElo(newVictimElo);

        killerData.setKills(killerData.getKills() + 1);
        killerData.setKillStreak(killerData.getKillStreak() + 1);
        if (killerData.getKillStreak() > killerData.getHighestKillStreak()) {
            killerData.setHighestKillStreak(killerData.getKillStreak());
        }
        killerData.setDeathStreak(0);

        victimData.setDeaths(victimData.getDeaths() + 1);
        victimData.setDeathStreak(victimData.getDeathStreak() + 1);
        victimData.setKillStreak(0);
        victimData.setLastKillerUUID(killer.getUniqueId().toString());
        victimData.setLastKilledTime(System.currentTimeMillis());
        victimData.setNoDeathStart(System.currentTimeMillis());

        if (antiFarmEnabled) logKill(killerData, victim.getUniqueId().toString());

        if (streakModuleEnabled) StreakManager.getInstance().checkMilestone(killer, killerData);

        if (Settings.MODULE_BOUNTY && BountyManager.getInstance().hasBounty(victimData)) {
            BountyManager.getInstance().claimBounties(killer, killerData, victimData);
        }

        PluginDataManager.invalidateTopCache();

        sendKillBroadcast(killer, killerData, victim, victimData, breakdown);

        if (breakdown.revengeBonusPct > 0) {
            String revengeMsg = MessageUtil.getMessage("revenge-kill",
                            "<gold>[Báo thù] <white>{player} <white>đã trả thù <red>{target}<white>!")
                    .replace("{player}", killer.getName())
                    .replace("{target}", victim.getName());
            MessageUtil.sendBroadcast(revengeMsg);
        }

        if (streakModuleEnabled && RankManager.getInstance().isWeak(victimData)) {
            String weakMsg = MessageUtil.getMessage("weak-status",
                            "<red>{player} <white>mang trạng thái <red>[Kẻ yếu]<white>!")
                    .replace("{player}", victim.getName());
            MessageUtil.sendBroadcast(weakMsg);
        }

        final String killerUuid = killer.getUniqueId().toString();
        final String victimUuid = victim.getUniqueId().toString();
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            PluginDataManager.savePlayerDatabaseToStorage(killerUuid);
            PluginDataManager.savePlayerDatabaseToStorage(victimUuid);
        });
    }

    public KillEloBreakdown calculateBreakdown(Player killer, PlayerData killerData,
                                               PlayerData victimData) {
        int baseElo = (int) Math.floor(victimData.getElo() * (Settings.ELO_KILL_PERCENT / 100.0));
        if (baseElo < 1) baseElo = 1;

        double multiplier = 1.0;
        String multiplierLabel = "";

        if (victimData.getElo() > killerData.getElo()) {
            multiplier = Settings.ELO_HIGH_MULTIPLIER;
            multiplierLabel = "high-elo ×" + String.format("%.1f", multiplier);
        } else {
            double lowThresholdPct = EloFile.get().getInt("low-elo-threshold", 50) / 100.0;
            if (victimData.getElo() < killerData.getElo() * lowThresholdPct) {
                multiplier = Settings.ELO_LOW_MULTIPLIER;
                multiplierLabel = "low-elo ×" + String.format("%.1f", multiplier);
            }
        }

        boolean isRevenge = isRevenge(killer, victimData);
        int revengeBonusPct = isRevenge ? Settings.ELO_REVENGE_BONUS_PERCENT : 0;
        double revengeMultiplier = 1.0 + (revengeBonusPct / 100.0);

        int nextStreak = killerData.getKillStreak() + 1;
        int streakBonusPct = Settings.MODULE_STREAKS
                ? StreakManager.getInstance().getStreakBonusPercent(nextStreak) : 0;
        double streakMultiplier = 1.0 + (streakBonusPct / 100.0);

        boolean isStreakBreak = Settings.MODULE_STREAKS && victimData.getKillStreak() >= 3;
        int brokenStreak = isStreakBreak ? victimData.getKillStreak() : 0;

        int eloGained;
        if (isStreakBreak) {
            eloGained = (int) Math.round(
                    baseElo * multiplier * revengeMultiplier * streakMultiplier
                            * (1 + victimData.getKillStreak() * 0.05));
        } else {
            eloGained = (int) Math.round(baseElo * multiplier * revengeMultiplier * streakMultiplier);
        }

        int weakPenalty = 0;
        if (Settings.MODULE_STREAKS && RankManager.getInstance().isWeak(victimData)) {
            int extraPct = StreaksFile.get().getInt("death-streak.extra-loss-percent", 5);
            weakPenalty = (int) Math.round(eloGained * (extraPct / 100.0));
        }
        int totalVictimLoss = eloGained + weakPenalty;

        return new KillEloBreakdown(baseElo, multiplier, multiplierLabel,
                revengeBonusPct, streakBonusPct, eloGained,
                weakPenalty, totalVictimLoss, isStreakBreak, brokenStreak);
    }

    private void sendKillBroadcast(Player killer, PlayerData killerData,
                                   Player victim, PlayerData victimData,
                                   KillEloBreakdown bd) {
        String killMsg = MessageUtil.getMessage("kill-broadcast",
                        "<reset>{rank_killer} <white>{killer} {elo_gained_detail} "
                                + "<white>đã chọc chết {rank_victim} <white>{victim} {elo_lost_detail}")
                .replace("{rank_killer}", RankManager.getInstance().getRankTag(killerData.getElo()))
                .replace("{killer}", killer.getName())
                .replace("{elo_gained}", String.valueOf(bd.eloGained))
                .replace("{elo_gained_detail}", bd.buildBreakdownString())
                .replace("{rank_victim}", RankManager.getInstance().getRankTag(victimData.getElo()))
                .replace("{victim}", victim.getName())
                .replace("{elo_lost}", String.valueOf(bd.totalVictimLoss))
                .replace("{elo_lost_detail}", bd.buildVictimLossString());
        MessageUtil.sendBroadcast(killMsg);
    }

    public void applyEloDecay(PlayerData data) {
        if (!Settings.MODULE_DECAY) return;

        long offlineMs = System.currentTimeMillis() - data.getLastOnline();
        long offlineDaysActual = offlineMs / (24L * 60 * 60 * 1000);

        if (offlineDaysActual > Settings.DECAY_OFFLINE_DAYS) {
            long decayDays = offlineDaysActual - Settings.DECAY_OFFLINE_DAYS;
            double totalDecayPct = Math.min(decayDays * Settings.DECAY_DAILY_PERCENT, Settings.DECAY_MAX_PERCENT) / 100.0;
            int lostElo = (int) Math.round(data.getElo() * totalDecayPct);
            int newElo = Math.max(Settings.ELO_MIN, data.getElo() - lostElo);
            if (lostElo > 0) {
                data.setElo(newElo);
                data.setLastOnline(System.currentTimeMillis());
            }
        }
    }

    public boolean isNewbie(PlayerData data) {
        if (!Settings.MODULE_PROTECTION) return false;
        long onlineMs = System.currentTimeMillis() - data.getFirstJoinTime();
        long onlineHours = onlineMs / (60L * 60 * 1000);
        return onlineHours < Settings.PROTECTION_NEWBIE_HOURS || data.getElo() < Settings.PROTECTION_NEWBIE_ELO;
    }

    private void applyKillStats(PlayerData killerData, PlayerData victimData) {
        killerData.setKills(killerData.getKills() + 1);
        killerData.setDeathStreak(0);
        victimData.setDeaths(victimData.getDeaths() + 1);
        victimData.setKillStreak(0);
    }

    private boolean isAntiFarm(PlayerData killerData, String victimUUID) {
        long now = System.currentTimeMillis();
        long oneHour = 60L * 60 * 1000;
        List<Long> timestamps = killerData.getOrCreateKillTimestamps(victimUUID);
        timestamps.removeIf(t -> (now - t) > oneHour);
        if (timestamps.isEmpty()) {
            killerData.getKillLog().remove(victimUUID);
        }
        return timestamps.size() >= Settings.ANTI_FARM_LIMIT_KILLS_PER_HOUR;
    }

    private void logKill(PlayerData killerData, String victimUUID) {
        killerData.getOrCreateKillTimestamps(victimUUID).add(System.currentTimeMillis());
    }

    private boolean isRevenge(Player killer, PlayerData victimData) {
        if (victimData.getLastKillerUUID() == null) return false;
        if (!victimData.getLastKillerUUID().equals(killer.getUniqueId().toString())) return false;
        long elapsed = System.currentTimeMillis() - victimData.getLastKilledTime();
        return elapsed <= (long) Settings.ELO_REVENGE_WINDOW_SECONDS * 1000;
    }
}

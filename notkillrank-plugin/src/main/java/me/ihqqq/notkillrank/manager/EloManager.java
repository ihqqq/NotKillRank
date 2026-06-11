package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.config.ConfigManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        PlayerData killerData = DataManager.getInstance().getOrCreate(killer);
        PlayerData victimData = DataManager.getInstance().getOrCreate(victim);

        boolean killerNewbie = isNewbie(killerData);
        boolean victimNewbie = isNewbie(victimData);
        if (victimNewbie || killerNewbie) {
            killerData.setKillStreak(killerData.getKillStreak() + 1);
            killerData.setKills(killerData.getKills() + 1);
            if (killerData.getKillStreak() > killerData.getHighestKillStreak()) {
                killerData.setHighestKillStreak(killerData.getKillStreak());
            }
            victimData.setDeaths(victimData.getDeaths() + 1);
            victimData.setKillStreak(0);
            victimData.setLastKillerUUID(killer.getUniqueId().toString());
            victimData.setLastKilledTime(System.currentTimeMillis());
            victimData.setNoDeathStart(System.currentTimeMillis());

            String msg = MessageUtil.getMessage("kill-no-elo",
                            "<reset>{rank_killer} <white>{killer} <white>đã chọc chết {rank_victim} <white>{victim} <gray>(bảo vệ người mới)")
                    .replace("{rank_killer}", RankManager.getInstance().getRankTag(killerData.getElo()))
                    .replace("{killer}", killer.getName())
                    .replace("{rank_victim}", RankManager.getInstance().getRankTag(victimData.getElo()))
                    .replace("{victim}", victim.getName());
            MessageUtil.sendBroadcast(msg);

            DataManager.getInstance().save(killer.getUniqueId().toString());
            DataManager.getInstance().save(victim.getUniqueId().toString());
            return;
        }

        if (isAntiFarm(killerData, victim.getUniqueId().toString())) {
            String limit = String.valueOf(getAntiFarmLimit());
            String anti = MessageUtil.getMessage("anti-farm",
                            "<gray>(Không nhận elo - Đã giết {victim} quá {limit} lần/giờ")
                    .replace("{victim}", victim.getName())
                    .replace("{limit}", limit);
            MessageUtil.sendMessage(killer, anti);

            killerData.setKills(killerData.getKills() + 1);
            victimData.setDeaths(victimData.getDeaths() + 1);
            DataManager.getInstance().save(killer.getUniqueId().toString());
            DataManager.getInstance().save(victim.getUniqueId().toString());
            return;
        }

        int baseElo = (int) Math.floor(victimData.getElo() * (getKillPercent() / 100.0));
        if (baseElo < 1) baseElo = 1;

        double multiplier = 1.0;
        if (victimData.getElo() > killerData.getElo()) {
            multiplier = getHighEloMultiplier();
        } else {
            double lowThresholdPct = getEloConfig().getInt("low-elo-threshold", 50) / 100.0;
            if (victimData.getElo() < killerData.getElo() * lowThresholdPct) {
                multiplier = getLowEloMultiplier();
            }
        }

        boolean isRevenge = isRevenge(killer, victimData);
        double revengeBonus = isRevenge ? 1.0 + (getRevengeBonusPercent() / 100.0) : 1.0;

        int streakBonusPct = StreakManager.getInstance().getStreakBonusPercent(killerData.getKillStreak() + 1);
        double streakMultiplier = 1.0 + (streakBonusPct / 100.0);

        int streakBreakBonusFromVictim = 0;
        if (victimData.getKillStreak() >= 3) {
            streakBreakBonusFromVictim = (int) Math.round(
                    baseElo * multiplier * revengeBonus * streakMultiplier
                            * (1 + victimData.getKillStreak() * 0.05));
        }

        int eloGained = (int) Math.round(baseElo * multiplier * revengeBonus * streakMultiplier);
        if (streakBreakBonusFromVictim > 0) eloGained = streakBreakBonusFromVictim;

        int deathPenalty = 0;
        if (RankManager.getInstance().isWeak(victimData)) {
            int extraPct = ConfigManager.getInstance().getStreaksConfig()
                    .getInt("death-streak.extra-loss-percent", 5);
            deathPenalty = (int) Math.round(eloGained * (extraPct / 100.0));
        }
        int totalVictimLoss = eloGained + deathPenalty;

        int minElo = getMinElo();
        int newKillerElo = killerData.getElo() + eloGained;
        int newVictimElo = Math.max(minElo, victimData.getElo() - totalVictimLoss);

        if (victimData.getKillStreak() >= 3) {
            StreakManager.getInstance().broadcastStreakBreak(killer, killerData, victimData, eloGained);
        }

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

        logKill(killerData, victim.getUniqueId().toString());
        StreakManager.getInstance().checkMilestone(killer, killerData);

        if (BountyManager.getInstance().hasBounty(victimData)) {
            BountyManager.getInstance().claimBounties(killer, victimData);
        }

        String killMsg = MessageUtil.getMessage("kill-broadcast",
                        "<reset>{rank_killer} <white>{killer} <green>(+{elo_gained}) <white>đã chọc chết {rank_victim} <white>{victim} <red>(-{elo_lost})")
                .replace("{rank_killer}", RankManager.getInstance().getRankTag(killerData.getElo()))
                .replace("{killer}", killer.getName())
                .replace("{elo_gained}", String.valueOf(eloGained))
                .replace("{rank_victim}", RankManager.getInstance().getRankTag(victimData.getElo()))
                .replace("{victim}", victim.getName())
                .replace("{elo_lost}", String.valueOf(totalVictimLoss));
        MessageUtil.sendBroadcast(killMsg);

        if (isRevenge) {
            String revengeMsg = MessageUtil.getMessage("revenge-kill",
                            "<gold>[Báo thù] <white>{player} <white>đã trả thù <red>{target}<white>!")
                    .replace("{player}", killer.getName())
                    .replace("{target}", victim.getName());
            MessageUtil.sendBroadcast(revengeMsg);
        }

        if (RankManager.getInstance().isWeak(victimData)) {
            String weakMsg = MessageUtil.getMessage("weak-status",
                            "<red>{player} <white>mang trạng thái <red>[Kẻ yếu]<white>!")
                    .replace("{player}", victim.getName());
            MessageUtil.sendBroadcast(weakMsg);
        }

        DataManager.getInstance().save(killer.getUniqueId().toString());
        DataManager.getInstance().save(victim.getUniqueId().toString());
    }

    public void applyEloDecay(PlayerData data) {
        FileConfiguration decay = ConfigManager.getInstance().getDecayConfig();
        int offlineDays = decay.getInt("offline-days", 7);
        int dailyPct = decay.getInt("daily-percent", 1);
        int maxPct = decay.getInt("max-percent", 10);
        int minElo = getMinElo();

        long offlineMs = System.currentTimeMillis() - data.getLastOnline();
        long offlineDaysActual = offlineMs / (24L * 60 * 60 * 1000);

        if (offlineDaysActual > offlineDays) {
            long decayDays = offlineDaysActual - offlineDays;
            double totalDecayPct = Math.min(decayDays * dailyPct, maxPct) / 100.0;
            int lostElo = (int) Math.round(data.getElo() * totalDecayPct);
            int newElo = Math.max(minElo, data.getElo() - lostElo);
            if (lostElo > 0) {
                data.setElo(newElo);
            }
        }
    }

    public boolean isNewbie(PlayerData data) {
        FileConfiguration prot = ConfigManager.getInstance().getProtectionConfig();
        int newbieHours = prot.getInt("newbie-hours", 10);
        int newbieElo = prot.getInt("newbie-protect-elo", 100);
        long onlineMs = System.currentTimeMillis() - data.getFirstJoinTime();
        long onlineHours = onlineMs / (60L * 60 * 1000);
        return onlineHours < newbieHours || data.getElo() < newbieElo;
    }

    private boolean isAntiFarm(PlayerData killerData, String victimUUID) {
        int limit = getAntiFarmLimit();
        long now = System.currentTimeMillis();
        long oneHour = 60L * 60 * 1000;

        List<Long> timestamps = killerData.getKillLog().getOrDefault(victimUUID, new ArrayList<>());
        timestamps.removeIf(t -> (now - t) > oneHour);
        killerData.getKillLog().put(victimUUID, timestamps);

        return timestamps.size() >= limit;
    }

    private void logKill(PlayerData killerData, String victimUUID) {
        List<Long> timestamps = killerData.getKillLog().computeIfAbsent(victimUUID, k -> new ArrayList<>());
        timestamps.add(System.currentTimeMillis());
    }

    private boolean isRevenge(Player killer, PlayerData victimData) {
        if (victimData.getLastKillerUUID() == null) return false;
        if (!victimData.getLastKillerUUID().equals(killer.getUniqueId().toString())) return false;
        int windowSeconds = getEloConfig().getInt("revenge-window-seconds", 300);
        long elapsed = System.currentTimeMillis() - victimData.getLastKilledTime();
        return elapsed <= (long) windowSeconds * 1000;
    }

    private FileConfiguration getEloConfig() {
        return ConfigManager.getInstance().getEloConfig();
    }

    private int getKillPercent() {
        return getEloConfig().getInt("kill-percent", 10);
    }

    private int getMinElo() {
        return getEloConfig().getInt("min-elo", 0);
    }

    private double getHighEloMultiplier() {
        return getEloConfig().getDouble("high-elo-multiplier", 1.5);
    }

    private double getLowEloMultiplier() {
        return getEloConfig().getDouble("low-elo-multiplier", 0.5);
    }

    private double getRevengeBonusPercent() {
        return getEloConfig().getInt("revenge-bonus-percent", 20);
    }

    private int getAntiFarmLimit() {
        return ConfigManager.getInstance().getAntiFarmConfig().getInt("limit-kills-per-hour", 3);
    }
}

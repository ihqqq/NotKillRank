package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.config.ConfigManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
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

    // ──────────────────────────────────────────────────────────────────────────
    // MAIN KILL HANDLER
    // ──────────────────────────────────────────────────────────────────────────

    public void processKill(Player killer, Player victim) {
        PlayerData killerData = DataManager.getInstance().getOrCreate(killer);
        PlayerData victimData = DataManager.getInstance().getOrCreate(victim);

        // ── Bảo vệ người mới ─────────────────────────────────────────────────
        boolean killerNewbie = isNewbie(killerData);
        boolean victimNewbie = isNewbie(victimData);

        if (victimNewbie || killerNewbie) {
            applyKillStats(killerData, victimData, killer);
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

            DataManager.getInstance().save(killer.getUniqueId().toString());
            DataManager.getInstance().save(victim.getUniqueId().toString());
            return;
        }

        // ── Anti-farm ────────────────────────────────────────────────────────
        boolean antiFarmEnabled = ModuleManager.getInstance().isEnabled(ModuleManager.Module.ANTI_FARM);
        if (antiFarmEnabled && isAntiFarm(killerData, victim.getUniqueId().toString())) {
            String limit = String.valueOf(getAntiFarmLimit());
            String anti = MessageUtil.getMessage("anti-farm",
                            "<gray>(Không nhận elo — Đã giết {victim} quá {limit} lần/giờ)")
                    .replace("{victim}", victim.getName())
                    .replace("{limit}", limit);
            MessageUtil.sendMessage(killer, anti);

            killerData.setKills(killerData.getKills() + 1);
            victimData.setDeaths(victimData.getDeaths() + 1);
            DataManager.getInstance().save(killer.getUniqueId().toString());
            DataManager.getInstance().save(victim.getUniqueId().toString());
            return;
        }

        // ── Tính elo ─────────────────────────────────────────────────────────
        KillEloBreakdown breakdown = calculateBreakdown(killer, killerData, victimData);

        // ── Phá streak ───────────────────────────────────────────────────────
        boolean streakModuleEnabled = ModuleManager.getInstance().isEnabled(ModuleManager.Module.STREAKS);
        if (streakModuleEnabled && breakdown.isStreakBreak) {
            StreakManager.getInstance().broadcastStreakBreak(killer, killerData, victimData,
                    breakdown.eloGained);
        }

        // ── Áp dụng thay đổi elo ─────────────────────────────────────────────
        int minElo = getMinElo();
        int newKillerElo = killerData.getElo() + breakdown.eloGained;
        int newVictimElo = Math.max(minElo, victimData.getElo() - breakdown.totalVictimLoss);

        killerData.setElo(newKillerElo);
        if (newKillerElo > killerData.getPeakElo()) killerData.setPeakElo(newKillerElo);
        victimData.setElo(newVictimElo);

        // ── Cập nhật stats ────────────────────────────────────────────────────
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

        // ── Log kill cho anti-farm ────────────────────────────────────────────
        if (antiFarmEnabled) {
            logKill(killerData, victim.getUniqueId().toString());
        }

        // ── Streak milestone ──────────────────────────────────────────────────
        if (streakModuleEnabled) {
            StreakManager.getInstance().checkMilestone(killer, killerData);
        }

        // ── Bounty ────────────────────────────────────────────────────────────
        if (ModuleManager.getInstance().isEnabled(ModuleManager.Module.BOUNTY)
                && BountyManager.getInstance().hasBounty(victimData)) {
            BountyManager.getInstance().claimBounties(killer, victimData);
        }

        DataManager.getInstance().invalidateTopCache();

        // ── Broadcast kill với breakdown ──────────────────────────────────────
        sendKillBroadcast(killer, killerData, victim, victimData, breakdown);

        // ── Báo thù thông báo riêng ───────────────────────────────────────────
        if (breakdown.revengeBonusPct > 0) {
            String revengeMsg = MessageUtil.getMessage("revenge-kill",
                            "<gold>[Báo thù] <white>{player} <white>đã trả thù <red>{target}<white>!")
                    .replace("{player}", killer.getName())
                    .replace("{target}", victim.getName());
            MessageUtil.sendBroadcast(revengeMsg);
        }

        // ── [Kẻ yếu] thông báo ───────────────────────────────────────────────
        if (streakModuleEnabled && RankManager.getInstance().isWeak(victimData)) {
            String weakMsg = MessageUtil.getMessage("weak-status",
                            "<red>{player} <white>mang trạng thái <red>[Kẻ yếu]<white>!")
                    .replace("{player}", victim.getName());
            MessageUtil.sendBroadcast(weakMsg);
        }

        DataManager.getInstance().save(killer.getUniqueId().toString());
        DataManager.getInstance().save(victim.getUniqueId().toString());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BREAKDOWN CALCULATION
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Tính toán đầy đủ breakdown elo cho một lần kill.
     * Gọi TRƯỚC KHI thay đổi bất kỳ stats nào (streak chưa +1 ở thời điểm này).
     */
    public KillEloBreakdown calculateBreakdown(Player killer, PlayerData killerData,
                                               PlayerData victimData) {
        int baseElo = (int) Math.floor(victimData.getElo() * (getKillPercent() / 100.0));
        if (baseElo < 1) baseElo = 1;

        double multiplier = 1.0;
        String multiplierLabel = "";
        boolean streakModuleEnabled = ModuleManager.getInstance().isEnabled(ModuleManager.Module.STREAKS);

        if (victimData.getElo() > killerData.getElo()) {
            multiplier = getHighEloMultiplier();
            multiplierLabel = "high-elo ×" + String.format("%.1f", multiplier);
        } else {
            double lowThresholdPct = getEloConfig().getInt("low-elo-threshold", 50) / 100.0;
            if (victimData.getElo() < killerData.getElo() * lowThresholdPct) {
                multiplier = getLowEloMultiplier();
                multiplierLabel = "low-elo ×" + String.format("%.1f", multiplier);
            }
        }

        boolean isRevenge = isRevenge(killer, victimData);
        int revengeBonusPct = isRevenge ? getRevengeBonusPercent() : 0;
        double revengeMultiplier = 1.0 + (revengeBonusPct / 100.0);

        int nextStreak = killerData.getKillStreak() + 1;
        int streakBonusPct = streakModuleEnabled
                ? StreakManager.getInstance().getStreakBonusPercent(nextStreak) : 0;
        double streakMultiplier = 1.0 + (streakBonusPct / 100.0);

        boolean isStreakBreak = streakModuleEnabled && victimData.getKillStreak() >= 3;
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
        if (streakModuleEnabled && RankManager.getInstance().isWeak(victimData)) {
            int extraPct = ConfigManager.getInstance().getStreaksConfig()
                    .getInt("death-streak.extra-loss-percent", 5);
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
        if (!ModuleManager.getInstance().isEnabled(ModuleManager.Module.DECAY)) return;

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
        if (!ModuleManager.getInstance().isEnabled(ModuleManager.Module.PROTECTION)) return false;

        FileConfiguration prot = ConfigManager.getInstance().getProtectionConfig();
        int newbieHours = prot.getInt("newbie-hours", 10);
        int newbieElo = prot.getInt("newbie-protect-elo", 100);
        long onlineMs = System.currentTimeMillis() - data.getFirstJoinTime();
        long onlineHours = onlineMs / (60L * 60 * 1000);
        return onlineHours < newbieHours || data.getElo() < newbieElo;
    }


    private void applyKillStats(PlayerData killerData, PlayerData victimData, Player killer) {
        killerData.setKillStreak(killerData.getKillStreak() + 1);
        killerData.setKills(killerData.getKills() + 1);
        if (killerData.getKillStreak() > killerData.getHighestKillStreak()) {
            killerData.setHighestKillStreak(killerData.getKillStreak());
        }
        victimData.setDeaths(victimData.getDeaths() + 1);
        victimData.setKillStreak(0);
    }

    private boolean isAntiFarm(PlayerData killerData, String victimUUID) {
        int limit = getAntiFarmLimit();
        long now = System.currentTimeMillis();
        long oneHour = 60L * 60 * 1000;

        List<Long> timestamps = killerData.getOrCreateKillTimestamps(victimUUID);
        timestamps.removeIf(t -> (now - t) > oneHour);

        return timestamps.size() >= limit;
    }

    private void logKill(PlayerData killerData, String victimUUID) {
        killerData.getOrCreateKillTimestamps(victimUUID).add(System.currentTimeMillis());
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

    private int getRevengeBonusPercent() {
        return getEloConfig().getInt("revenge-bonus-percent", 20);
    }

    private int getAntiFarmLimit() {
        return ConfigManager.getInstance().getAntiFarmConfig().getInt("limit-kills-per-hour", 3);
    }
}
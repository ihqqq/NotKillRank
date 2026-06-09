package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Set;
import java.util.TreeSet;

public class StreakManager {

    private static StreakManager instance;

    public StreakManager() {
        instance = this;
    }

    public static StreakManager getInstance() {
        return instance;
    }

    public int getStreakBonusPercent(int streak) {
        Set<Integer> milestones = getMilestones();
        int bestBonus = 0;
        for (int m : milestones) {
            if (streak >= m) {
                int bonus = NotKillRank.getInstance().getConfig()
                        .getInt("kill-streaks." + m + ".bonus-percent", 0);
                if (bonus > bestBonus) bestBonus = bonus;
            }
        }
        return bestBonus;
    }

    public void checkMilestone(Player killer, PlayerData killerData) {
        int streak = killerData.getKillStreak();
        Set<Integer> milestones = getMilestones();

        for (int m : milestones) {
            if (streak == m) {
                String broadcastMsg = NotKillRank.getInstance().getConfig()
                        .getString("kill-streaks." + m + ".broadcast-message", "");
                boolean doBroadcast = NotKillRank.getInstance().getConfig()
                        .getBoolean("kill-streaks." + m + ".broadcast", false);
                boolean doSound = NotKillRank.getInstance().getConfig()
                        .getBoolean("kill-streaks." + m + ".sound", false);
                boolean doTitle = NotKillRank.getInstance().getConfig()
                        .getBoolean("kill-streaks." + m + ".show-title", false);

                if (doBroadcast && !broadcastMsg.isEmpty()) {
                    String name = NotKillRank.getInstance().getConfig()
                            .getString("kill-streaks." + m + ".name", "");
                    String msg = broadcastMsg
                            .replace("{player}", killer.getName())
                            .replace("{streak}", String.valueOf(streak))
                            .replace("{name}", name);
                    MessageUtil.sendBroadcast(msg);
                }

                if (doSound) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.playSound(online.getLocation(),
                                Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                    }
                }

                if (doTitle) {
                    String titleText = NotKillRank.getInstance().getConfig()
                            .getString("kill-streaks." + m + ".title-text",
                                    "<red><bold>KILL STREAK");
                    String subtitleText = NotKillRank.getInstance().getConfig()
                            .getString("kill-streaks." + m + ".subtitle-text", "");

                    String titleStr = titleText
                            .replace("{player}", killer.getName())
                            .replace("{streak}", String.valueOf(streak));
                    String subtitleStr = subtitleText
                            .replace("{player}", killer.getName())
                            .replace("{streak}", String.valueOf(streak));

                    Title adventureTitle = Title.title(
                            MessageUtil.parse(titleStr),
                            MessageUtil.parse(subtitleStr),
                            Title.Times.times(
                                    Duration.ofMillis(500),
                                    Duration.ofMillis(3500),
                                    Duration.ofMillis(1000)
                            )
                    );
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.showTitle(adventureTitle);
                    }
                }
                break;
            }
        }
    }

    public void broadcastStreakBreak(Player breaker, PlayerData breakerData,
                                     PlayerData victimData, int eloGained) {
        if (victimData.getKillStreak() < 3) return;
        String msg = NotKillRank.getInstance().getConfig()
                .getString("messages.streak-break",
                        "<yellow>{breaker} <white>da cham dut chuoi <red>{streak} kill "
                                + "<white>cua <yellow>{victim}<white>! <green>(+{elo} elo)")
                .replace("{breaker}", breaker.getName())
                .replace("{streak}", String.valueOf(victimData.getKillStreak()))
                .replace("{victim}", victimData.getName())
                .replace("{elo}", String.valueOf(eloGained));
        MessageUtil.sendBroadcast(msg);
    }

    public TreeSet<Integer> getMilestones() {
        TreeSet<Integer> milestones = new TreeSet<>();
        var section = NotKillRank.getInstance().getConfig()
                .getConfigurationSection("kill-streaks");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    milestones.add(Integer.parseInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }
        return milestones;
    }
}

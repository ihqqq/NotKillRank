package me.ihqqq.notkillrank.support;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import org.bukkit.entity.Player;

public class PlaceholderAPISupport extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "notkillrank";
    }

    @Override
    public String getAuthor() {
        return "ihqqq";
    }

    @Override
    public String getVersion() {
        return NotKillRank.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return NotKillRank.getInstance() != null && NotKillRank.getInstance().isEnabled();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        PlayerData data = DataManager.getInstance().getOrCreate(player);
        if (data == null) return "";

        return switch (identifier.toLowerCase()) {
            case "elo" -> String.valueOf(data.getElo());
            case "rank" -> RankManager.getInstance().getRankTag(data.getElo());
            case "streak_tag" -> RankManager.getInstance().getStreakTag(data);
            case "kills" -> String.valueOf(data.getKills());
            case "deaths" -> String.valueOf(data.getDeaths());
            case "kd" -> data.getDeaths() == 0
                    ? String.valueOf(data.getKills())
                    : String.format("%.2f", (double) data.getKills() / data.getDeaths());
            case "kill_streak" -> String.valueOf(data.getKillStreak());
            case "death_streak" -> String.valueOf(data.getDeathStreak());
            case "highest_streak" -> String.valueOf(data.getHighestKillStreak());
            case "peak_elo" -> String.valueOf(data.getPeakElo());
            case "rank_with_tag" -> {
                String rank = RankManager.getInstance().getRankTag(data.getElo());
                String streak = RankManager.getInstance().getStreakTag(data);
                yield rank + (streak.isEmpty() ? "" : " " + streak);
            }
            case "is_weak" -> RankManager.getInstance().isWeak(data) ? "true" : "false";
            case "bounty" -> String.valueOf(
                    data.getBounties().values().stream().mapToInt(Integer::intValue).sum());
            default -> null;
        };
    }
}

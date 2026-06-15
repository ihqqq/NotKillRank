package me.ihqqq.notkillrank.support;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.entity.Player;

public class PlaceholderAPISupport extends PlaceholderExpansion {

    @Override public String getIdentifier() { return "notkillrank"; }
    @Override public String getAuthor()     { return "ihqqq"; }
    @Override public String getVersion()    { return NotKillRank.plugin.getDescription().getVersion(); }
    @Override public boolean persist()      { return true; }

    @Override
    public boolean canRegister() {
        return Settings.MODULE_PLACEHOLDERAPI
                && NotKillRank.plugin != null
                && NotKillRank.plugin.isEnabled();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        PlayerData data = PluginDataManager.getOrCreate(player);
        if (data == null) return "";

        return switch (identifier.toLowerCase()) {
            case "elo"            -> String.valueOf(data.getElo());
            case "rank"           -> mm2legacy(RankManager.getInstance().getRankTag(data.getElo()));
            case "streak_tag"     -> mm2legacy(RankManager.getInstance().getStreakTag(data));
            case "kills"          -> String.valueOf(data.getKills());
            case "deaths"         -> String.valueOf(data.getDeaths());
            case "kd"             -> data.getDeaths() == 0
                    ? String.valueOf(data.getKills())
                    : String.format("%.2f", (double) data.getKills() / data.getDeaths());
            case "kill_streak"    -> String.valueOf(data.getKillStreak());
            case "death_streak"   -> String.valueOf(data.getDeathStreak());
            case "highest_streak" -> String.valueOf(data.getHighestKillStreak());
            case "peak_elo"       -> String.valueOf(data.getPeakElo());
            case "rank_with_tag"  -> {
                String rank   = RankManager.getInstance().getRankTag(data.getElo());
                String streak = RankManager.getInstance().getStreakTag(data);
                yield mm2legacy(rank + (streak.isEmpty() ? "" : " " + streak));
            }
            case "is_weak" -> RankManager.getInstance().isWeak(data) ? "true" : "false";
            case "bounty"  -> String.valueOf(
                    data.getBounties().values().stream().mapToInt(Integer::intValue).sum());
            default -> null;
        };
    }

    private static String mm2legacy(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) return "";
        return MessageUtil.toLegacyString(miniMessage);
    }
}

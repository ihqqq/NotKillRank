package me.ihqqq.notkillrank.command;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class StatsCommand implements CommandExecutor, TabCompleter {

    public StatsCommand() {
        NotKillRank.getInstance().getCommand("stats").setExecutor(this);
        NotKillRank.getInstance().getCommand("stats").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        PlayerData data;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /stats <player>");
                return true;
            }
            data = DataManager.getInstance().getOrCreate(player);
        } else {
            data = DataManager.getInstance().getByName(args[0]);
            if (data == null) {
                MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                        .getString("messages.player-not-found",
                                "<red>Không tìm thấy người chơi <yellow>{player}<red>!")
                        .replace("{player}", args[0]));
                return true;
            }
        }

        String rank = RankManager.getInstance().getRankTag(data.getElo());
        String streakTag = RankManager.getInstance().getStreakTag(data);
        String streakPart = streakTag.isEmpty() ? "" : " " + streakTag;

        MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                .getString("messages.stats-header",
                        "<dark_gray><strikethrough>          </strikethrough> <gold>Thống kê của {player} <dark_gray><strikethrough>          </strikethrough>")
                .replace("{player}", data.getName()));

        MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                .getString("messages.stats-elo", "<white>Elo: <green>{elo} <white>| Hạng: {rank}")
                .replace("{elo}", String.valueOf(data.getElo()))
                .replace("{rank}", rank + streakPart));

        String kd = data.getDeaths() == 0
                ? String.valueOf(data.getKills())
                : String.format("%.2f", (double) data.getKills() / data.getDeaths());
        MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                .getString("messages.stats-kd",
                        "<white>K/D: <yellow>{kd} <gray>({kills} kill / {deaths} death)")
                .replace("{kd}", kd)
                .replace("{kills}", String.valueOf(data.getKills()))
                .replace("{deaths}", String.valueOf(data.getDeaths())));

        MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                .getString("messages.stats-streak",
                        "<white>Chuỗi kill cao nhất: <red>{streak}")
                .replace("{streak}", String.valueOf(data.getHighestKillStreak())));

        MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                .getString("messages.stats-peak", "<white>Elo đỉnh mùa: <gold>{peak}")
                .replace("{peak}", String.valueOf(data.getPeakElo())));

        int totalBounty = data.getBounties().values().stream().mapToInt(Integer::intValue).sum();
        if (totalBounty > 0) {
            MessageUtil.sendMessage(sender, "<white>Bounty trên đầu: <red>" + totalBounty + " elo");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    completions.add(p.getName());
            }
        }
        return completions;
    }
}

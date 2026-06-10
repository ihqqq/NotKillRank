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

public class EloCommand implements CommandExecutor, TabCompleter {

    public EloCommand() {
        NotKillRank.getInstance().getCommand("elo").setExecutor(this);
        NotKillRank.getInstance().getCommand("elo").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /elo <player>");
                return true;
            }
            showElo(sender, DataManager.getInstance().getOrCreate(player));
        } else {
            PlayerData data = DataManager.getInstance().getByName(args[0]);
            if (data == null) {
                MessageUtil.sendMessage(sender, MessageUtil.getMessage("player-not-found",
                                "<red>Không tìm thấy người chơi <yellow>{player}<red>!")
                        .replace("{player}", args[0]));
                return true;
            }
            showElo(sender, data);
        }
        return true;
    }

    private void showElo(CommandSender sender, PlayerData data) {
        String rank = RankManager.getInstance().getRankTag(data.getElo());
        String streakTag = RankManager.getInstance().getStreakTag(data);
        String streakPart = streakTag.isEmpty() ? "" : " " + streakTag;
        MessageUtil.sendMessage(sender, MessageUtil.getMessage("elo-info",
                        "<gold>{player} <white>— Elo: <green>{elo} <white>| Hạng: {rank}")
                .replace("{player}", data.getName())
                .replace("{elo}", String.valueOf(data.getElo()))
                .replace("{rank}", rank + streakPart));
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

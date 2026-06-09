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
import java.util.Arrays;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {

    public AdminCommand() {
        NotKillRank.getInstance().getCommand("notkillrank").setExecutor(this);
        NotKillRank.getInstance().getCommand("notkillrank").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("notkillrank.admin")) {
            MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                    .getString("messages.no-permission",
                            "<red>Bạn không có quyền dùng lệnh này!"));
            return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                NotKillRank.getInstance().reloadConfig();
                RankManager.getInstance().reload();
                MessageUtil.sendMessage(sender,
                        MessageUtil.getPrefix() + "<green>Cấu hình đã được tải lại!");
            }
            case "reset" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, "<yellow>Cách dùng: <white>/nkr reset <player>");
                    return true;
                }
                PlayerData data = DataManager.getInstance().getByName(args[1]);
                if (data == null) {
                    MessageUtil.sendMessage(sender, notFound(args[1])); return true;
                }
                int startElo = NotKillRank.getInstance().getConfig().getInt("elo.start-elo", 1000);
                data.setElo(startElo); data.setKills(0); data.setDeaths(0);
                data.setKillStreak(0); data.setDeathStreak(0);
                data.setHighestKillStreak(0); data.setPeakElo(startElo);
                DataManager.getInstance().save(data.getUUID());
                MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                        + "<green>Đã reset dữ liệu của <yellow>" + data.getName() + "<green>!");
            }
            case "setelo" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender,
                            "<yellow>Cách dùng: <white>/nkr setelo <player> <elo>");
                    return true;
                }
                PlayerData data = DataManager.getInstance().getByName(args[1]);
                if (data == null) {
                    MessageUtil.sendMessage(sender, notFound(args[1])); return true;
                }
                try {
                    int elo = Integer.parseInt(args[2]);
                    if (elo < 0) throw new NumberFormatException();
                    data.setElo(elo);
                    if (elo > data.getPeakElo()) data.setPeakElo(elo);
                    DataManager.getInstance().save(data.getUUID());
                    MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                            + "<green>Đã set elo của <yellow>" + data.getName()
                            + " <green>thành <gold>" + elo + "<green>!");
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "<red>Elo không hợp lệ!");
                }
            }
            case "info" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender,
                            "<yellow>Cách dùng: <white>/nkr info <player>");
                    return true;
                }
                PlayerData data = DataManager.getInstance().getByName(args[1]);
                if (data == null) {
                    MessageUtil.sendMessage(sender, notFound(args[1])); return true;
                }
                MessageUtil.sendMessage(sender, "<gold>--- " + data.getName() + " ---");
                MessageUtil.sendMessage(sender, "<white>UUID: <gray>" + data.getUUID());
                MessageUtil.sendMessage(sender, "<white>Elo: <green>" + data.getElo()
                        + " <white>| Peak: <gold>" + data.getPeakElo());
                MessageUtil.sendMessage(sender, "<white>Kills/Deaths: <yellow>"
                        + data.getKills() + "<white>/<red>" + data.getDeaths());
                MessageUtil.sendMessage(sender, "<white>Kill Streak: <red>" + data.getKillStreak()
                        + " <white>| Death Streak: <dark_red>" + data.getDeathStreak());
                MessageUtil.sendMessage(sender, "<white>Hạng: "
                        + RankManager.getInstance().getRankTag(data.getElo()));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "<gold>--- NotKillRank Admin ---");
        MessageUtil.sendMessage(sender, "<yellow>/nkr reload <white>— Tải lại config");
        MessageUtil.sendMessage(sender,
                "<yellow>/nkr reset <player> <white>— Reset dữ liệu người chơi");
        MessageUtil.sendMessage(sender,
                "<yellow>/nkr setelo <player> <elo> <white>— Set elo người chơi");
        MessageUtil.sendMessage(sender,
                "<yellow>/nkr info <player> <white>— Xem thông tin chi tiết");
    }

    private String notFound(String name) {
        return NotKillRank.getInstance().getConfig()
                .getString("messages.player-not-found",
                        "<red>Không tìm thấy người chơi <yellow>{player}<red>!")
                .replace("{player}", name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("notkillrank.admin")) return new ArrayList<>();
        if (args.length == 1) return Arrays.asList("reload", "reset", "setelo", "info");
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                    names.add(p.getName());
            }
            return names;
        }
        return new ArrayList<>();
    }
}

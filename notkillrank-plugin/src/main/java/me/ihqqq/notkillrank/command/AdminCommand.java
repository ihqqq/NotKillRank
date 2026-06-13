package me.ihqqq.notkillrank.command;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.manager.ModuleManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
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
        NotKillRank.plugin.getCommand("notkillrank").setExecutor(this);
        NotKillRank.plugin.getCommand("notkillrank").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("notkillrank.admin")) {
            MessageUtil.sendMessage(sender, MessageUtil.getMessage("no-permission",
                    "<red>Bạn không có quyền sử dụng lệnh này!"));
            return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                NotKillRank.reload();
                MessageUtil.sendMessage(sender,
                        MessageUtil.getPrefix() + "<green>Cấu hình và trạng thái module đã được tải lại!");
            }
            case "modules" -> showModuleStatus(sender);
            case "reset" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, "<yellow>Cách dùng: <white>/nkr reset <player>");
                    return true;
                }
                PlayerData data = PluginDataManager.getPlayerDatabaseByName(args[1]);
                if (data == null) { MessageUtil.sendMessage(sender, notFound(args[1])); return true; }
                data.setElo(Settings.ELO_START);
                data.setKills(0); data.setDeaths(0);
                data.setKillStreak(0); data.setDeathStreak(0);
                data.setHighestKillStreak(0); data.setPeakElo(Settings.ELO_START);
                data.getKillLog().clear();
                data.getBounties().clear();
                saveAsync(data.getUUID());
                MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                        + "<green>Đã reset dữ liệu của <yellow>" + data.getName() + "<green>!");
            }
            case "setelo" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "<yellow>Cách dùng: <white>/nkr setelo <player> <elo>");
                    return true;
                }
                PlayerData data = PluginDataManager.getPlayerDatabaseByName(args[1]);
                if (data == null) { MessageUtil.sendMessage(sender, notFound(args[1])); return true; }
                try {
                    int elo = Integer.parseInt(args[2]);
                    if (elo < 0) throw new NumberFormatException();
                    int oldElo = data.getElo();
                    data.setElo(elo);
                    if (elo > data.getPeakElo()) data.setPeakElo(elo);
                    saveAsync(data.getUUID());
                    triggerRankUpIfOnline(data, oldElo, elo);
                    MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                            + "<green>Đã set elo của <yellow>" + data.getName()
                            + " <green>thành <gold>" + elo + "<green>!");
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "<red>Elo không hợp lệ!");
                }
            }
            case "give" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "<yellow>Cách dùng: <white>/nkr give <player> <elo>");
                    return true;
                }
                PlayerData data = PluginDataManager.getPlayerDatabaseByName(args[1]);
                if (data == null) { MessageUtil.sendMessage(sender, notFound(args[1])); return true; }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) throw new NumberFormatException();
                    int oldElo = data.getElo();
                    int newElo = (int) Math.min((long) oldElo + amount, Integer.MAX_VALUE);
                    data.setElo(newElo);
                    if (newElo > data.getPeakElo()) data.setPeakElo(newElo);
                    saveAsync(data.getUUID());
                    triggerRankUpIfOnline(data, oldElo, newElo);
                    MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                            + "<green>Đã cộng <gold>" + amount + " elo <green>cho <yellow>"
                            + data.getName() + "<green>! (Elo mới: <gold>" + newElo + "<green>)");
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "<red>Số lượng elo không hợp lệ! Phải là số nguyên dương.");
                }
            }
            case "take" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "<yellow>Cách dùng: <white>/nkr take <player> <elo>");
                    return true;
                }
                PlayerData data = PluginDataManager.getPlayerDatabaseByName(args[1]);
                if (data == null) { MessageUtil.sendMessage(sender, notFound(args[1])); return true; }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (amount <= 0) throw new NumberFormatException();
                    int newElo = Math.max(Settings.ELO_MIN, data.getElo() - amount);
                    int actualTaken = data.getElo() - newElo;
                    data.setElo(newElo);
                    saveAsync(data.getUUID());
                    MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                            + "<red>Đã trừ <gold>" + actualTaken + " elo <red>của <yellow>"
                            + data.getName() + "<red>! (Elo mới: <gold>" + newElo + "<red>)");
                } catch (NumberFormatException e) {
                    MessageUtil.sendMessage(sender, "<red>Số lượng elo không hợp lệ! Phải là số nguyên dương.");
                }
            }
            case "info" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, "<yellow>Cách dùng: <white>/nkr info <player>");
                    return true;
                }
                PlayerData data = PluginDataManager.getPlayerDatabaseByName(args[1]);
                if (data == null) { MessageUtil.sendMessage(sender, notFound(args[1])); return true; }
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

    private void showModuleStatus(CommandSender sender) {
        MessageUtil.sendMessage(sender, "<gold>--- Trạng thái Module ---");
        for (ModuleManager.Module m : ModuleManager.Module.values()) {
            boolean on = m.isEnabled();
            String status = on ? "<green>BẬT" : "<red>TẮT";
            MessageUtil.sendMessage(sender, "  <white>" + m.name().toLowerCase() + ": " + status);
        }
        MessageUtil.sendMessage(sender, "<gray>Thay đổi trong <white>config.yml <gray>→ <white>/nkr reload");
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "<gold>--- NotKillRank Admin ---");
        MessageUtil.sendMessage(sender, "<yellow>/nkr reload <white>— Tải lại config & module");
        MessageUtil.sendMessage(sender, "<yellow>/nkr modules <white>— Xem trạng thái các module");
        MessageUtil.sendMessage(sender, "<yellow>/nkr reset <player> <white>— Reset toàn bộ dữ liệu người chơi");
        MessageUtil.sendMessage(sender, "<yellow>/nkr setelo <player> <elo> <white>— Set elo chính xác");
        MessageUtil.sendMessage(sender, "<yellow>/nkr give <player> <elo> <white>— Cộng elo cho người chơi");
        MessageUtil.sendMessage(sender, "<yellow>/nkr take <player> <elo> <white>— Trừ elo của người chơi");
        MessageUtil.sendMessage(sender, "<yellow>/nkr info <player> <white>— Xem thông tin chi tiết");
    }

    private void saveAsync(String uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin,
                () -> PluginDataManager.savePlayerDatabaseToStorage(uuid));
    }

    private void triggerRankUpIfOnline(PlayerData data, int oldElo, int newElo) {
        Player target = Bukkit.getPlayer(java.util.UUID.fromString(data.getUUID()));
        if (target == null || !target.isOnline()) return;
        RankManager.getInstance().checkRankUp(target, data, oldElo, newElo);
    }

    private String notFound(String name) {
        return MessageUtil.getMessage("player-not-found",
                        "<red>Không tìm thấy người chơi <yellow>{player}<red>!")
                .replace("{player}", name);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("notkillrank.admin")) return new ArrayList<>();
        if (args.length == 1)
            return Arrays.asList("reload", "modules", "reset", "setelo", "give", "take", "info");
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")
                && !args[0].equalsIgnoreCase("modules")) {
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

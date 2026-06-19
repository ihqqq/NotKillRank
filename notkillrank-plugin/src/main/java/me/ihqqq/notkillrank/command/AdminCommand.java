package me.ihqqq.notkillrank.command;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.manager.ModuleManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import me.ihqqq.notkillrank.webhook.WebhookManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
                        MessageUtil.getPrefix() + MessageUtil.getMessage("admin-reload",
                                "<green>✔ Cấu hình và trạng thái module đã được tải lại!"));
            }
            case "modules" -> showModuleStatus(sender);
            case "reset" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-usage-reset",
                            "<yellow>Cách dùng: <white>/nkr reset <player>"));
                    return true;
                }
                lookupPlayerAsync(sender, args[1], data -> {
                    data.setElo(Settings.ELO_START);
                    data.setKills(0); data.setDeaths(0);
                    data.setKillStreak(0); data.setDeathStreak(0);
                    data.setHighestKillStreak(0); data.setPeakElo(Settings.ELO_START);
                    data.getKillLog().clear();
                    data.getBounties().clear();
                    saveAsync(data.getUUID());
                    MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                            + MessageUtil.getMessage("admin-reset-done",
                                    "<green>✔ Đã reset dữ liệu của <yellow>{player}<green>!")
                            .replace("{player}", data.getName()));
                });
            }
            case "setelo" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-usage-setelo",
                            "<yellow>Cách dùng: <white>/nkr setelo <player> <elo>"));
                    return true;
                }
                final String rawElo = args[2];
                lookupPlayerAsync(sender, args[1], data -> {
                    try {
                        int elo = Integer.parseInt(rawElo);
                        if (elo < 0) throw new NumberFormatException();
                        int oldElo = data.getElo();
                        data.setElo(elo);
                        if (elo > data.getPeakElo()) data.setPeakElo(elo);
                        saveAsync(data.getUUID());
                        triggerRankUpIfOnline(data, oldElo, elo);
                        MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                                + MessageUtil.getMessage("admin-setelo-done",
                                        "<green>✔ Đã set elo của <yellow>{player} <green>thành <gold>{elo}<green>!")
                                .replace("{player}", data.getName())
                                .replace("{elo}", String.valueOf(elo)));
                    } catch (NumberFormatException e) {
                        MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-invalid-elo",
                                "<red>⚠ Elo không hợp lệ! Phải là số nguyên không âm."));
                    }
                });
            }
            case "give" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-usage-give",
                            "<yellow>Cách dùng: <white>/nkr give <player> <elo>"));
                    return true;
                }
                final String rawAmount = args[2];
                lookupPlayerAsync(sender, args[1], data -> {
                    try {
                        int amount = Integer.parseInt(rawAmount);
                        if (amount <= 0) throw new NumberFormatException();
                        int oldElo = data.getElo();
                        int newElo = (int) Math.min((long) oldElo + amount, Integer.MAX_VALUE);
                        data.setElo(newElo);
                        if (newElo > data.getPeakElo()) data.setPeakElo(newElo);
                        saveAsync(data.getUUID());
                        triggerRankUpIfOnline(data, oldElo, newElo);
                        MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                                + MessageUtil.getMessage("admin-give-done",
                                        "<green>✔ Đã cộng <gold>+{amount} elo <green>cho <yellow>{player}<green>! (Elo mới: <gold>{new_elo}<green>)")
                                .replace("{amount}", String.valueOf(amount))
                                .replace("{player}", data.getName())
                                .replace("{new_elo}", String.valueOf(newElo)));
                    } catch (NumberFormatException e) {
                        MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-invalid-amount",
                                "<red>⚠ Số lượng elo không hợp lệ! Phải là số nguyên dương."));
                    }
                });
            }
            case "take" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-usage-take",
                            "<yellow>Cách dùng: <white>/nkr take <player> <elo>"));
                    return true;
                }
                final String rawAmount = args[2];
                lookupPlayerAsync(sender, args[1], data -> {
                    try {
                        int amount = Integer.parseInt(rawAmount);
                        if (amount <= 0) throw new NumberFormatException();
                        int newElo = Math.max(Settings.ELO_MIN, data.getElo() - amount);
                        int actualTaken = data.getElo() - newElo;
                        data.setElo(newElo);
                        saveAsync(data.getUUID());
                        MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                                + MessageUtil.getMessage("admin-take-done",
                                        "<red>✔ Đã trừ <gold>{amount} elo <red>của <yellow>{player}<red>! (Elo mới: <gold>{new_elo}<red>)")
                                .replace("{amount}", String.valueOf(actualTaken))
                                .replace("{player}", data.getName())
                                .replace("{new_elo}", String.valueOf(newElo)));
                    } catch (NumberFormatException e) {
                        MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-invalid-amount",
                                "<red>⚠ Số lượng elo không hợp lệ! Phải là số nguyên dương."));
                    }
                });
            }
            case "info" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-usage-info",
                            "<yellow>Cách dùng: <white>/nkr info <player>"));
                    return true;
                }
                lookupPlayerAsync(sender, args[1], data ->
                        MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-info",
                                        "<gold>--- {player} ---\n<white>UUID: <gray>{uuid}\n"
                                                + "<white>Elo: <green>{elo} <white>| Peak: <gold>{peak}\n"
                                                + "<white>Kills/Deaths: <yellow>{kills}<white>/<red>{deaths}\n"
                                                + "<white>Kill Streak: <red>{streak} <white>| Death Streak: <dark_red>{death_streak}\n"
                                                + "<white>Hạng: {rank}")
                                .replace("{player}", data.getName())
                                .replace("{uuid}", data.getUUID())
                                .replace("{elo}", String.valueOf(data.getElo()))
                                .replace("{peak}", String.valueOf(data.getPeakElo()))
                                .replace("{kills}", String.valueOf(data.getKills()))
                                .replace("{deaths}", String.valueOf(data.getDeaths()))
                                .replace("{streak}", String.valueOf(data.getKillStreak()))
                                .replace("{death_streak}", String.valueOf(data.getDeathStreak()))
                                .replace("{rank}", RankManager.getInstance().getRankTag(data.getElo())))
                );
            }
            case "webhook" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("top-elo")) {
                    MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-usage-webhook",
                            "<yellow>Cách dùng: <white>/nkr webhook top-elo"));
                    return true;
                }
                WebhookManager.getInstance().sendTopElo();
                MessageUtil.sendMessage(sender, MessageUtil.getPrefix()
                        + MessageUtil.getMessage("admin-webhook-topelo-sent",
                        "<green>✔ Đã gửi Top ELO webhook lên Discord!"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void lookupPlayerAsync(CommandSender sender, String name, Consumer<PlayerData> onMain) {
        PlayerData fast = PluginDataManager.getPlayerDatabaseByNameNoIO(name);
        if (fast != null) {
            onMain.accept(fast);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            PlayerData data = PluginDataManager.getPlayerDatabaseByName(name);
            Bukkit.getScheduler().runTask(NotKillRank.plugin, () -> {
                if (data == null) {
                    MessageUtil.sendMessage(sender, notFound(name));
                    return;
                }
                onMain.accept(data);
            });
        });
    }

    private void showModuleStatus(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-modules-header",
                "<gold>--- Trạng thái Module ---"));
        for (ModuleManager.Module m : ModuleManager.Module.values()) {
            boolean on = m.isEnabled();
            String status = on ? "<green>BẬT" : "<red>TẮT";
            MessageUtil.sendMessage(sender, "  <white>" + m.name().toLowerCase() + ": " + status);
        }
        MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-modules-hint",
                "<gray>Thay đổi trong <white>config.yml <gray>→ <white>/nkr reload"));
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, MessageUtil.getMessage("admin-help",
                "<gold>--- NotKillRank Admin ---\n"
                        + "<yellow>/nkr reload <white>— Tải lại config & module\n"
                        + "<yellow>/nkr modules <white>— Xem trạng thái các module\n"
                        + "<yellow>/nkr reset <player> <white>— Reset toàn bộ dữ liệu người chơi\n"
                        + "<yellow>/nkr setelo <player> <elo> <white>— Set elo chính xác\n"
                        + "<yellow>/nkr give <player> <elo> <white>— Cộng elo cho người chơi\n"
                        + "<yellow>/nkr take <player> <elo> <white>— Trừ elo của người chơi\n"
                        + "<yellow>/nkr info <player> <white>— Xem thông tin chi tiết\n"
                        + "<yellow>/nkr webhook top-elo <white>— Gửi Top ELO lên Discord ngay"));
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
            return Arrays.asList("reload", "modules", "reset", "setelo", "give", "take", "info", "webhook");
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

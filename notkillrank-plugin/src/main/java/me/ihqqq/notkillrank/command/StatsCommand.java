package me.ihqqq.notkillrank.command;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.inventory.StatsInventory;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import me.ihqqq.notkillrank.webhook.SkinUtil;
import me.ihqqq.notkillrank.webhook.WebhookManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatsCommand implements CommandExecutor, TabCompleter {

    public StatsCommand() {
        NotKillRank.plugin.getCommand("stats").setExecutor(this);
        NotKillRank.plugin.getCommand("stats").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1 && (args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Lệnh này chỉ dùng được trong game.");
                return true;
            }
            StatsInventory.open(player, 0);
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Usage: /stats <player>");
                return true;
            }
            PlayerData data = PluginDataManager.getOrCreate(player);
            sendStats(sender, data);
            return true;
        }

        final String targetName = args[0];

        PlayerData cached = PluginDataManager.getPlayerDatabaseByNameNoIO(targetName);
        if (cached != null) {
            sendStats(sender, cached);
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            PlayerData data = PluginDataManager.getPlayerDatabaseByName(targetName);
            Bukkit.getScheduler().runTask(NotKillRank.plugin, () -> {
                if (data == null) {
                    MessageUtil.sendMessage(sender, MessageUtil.getMessage("player-not-found",
                                    "<red>Không tìm thấy người chơi <yellow>{player}<red>!")
                            .replace("{player}", targetName));
                    return;
                }
                sendStats(sender, data);
            });
        });

        return true;
    }

    private void sendStats(CommandSender sender, PlayerData data) {
        String rank      = RankManager.getInstance().getRankTag(data.getElo());
        String streakTag = RankManager.getInstance().getStreakTag(data);
        String streakPart = streakTag.isEmpty() ? "" : " " + streakTag;
        String kd = data.getDeaths() == 0
                ? String.valueOf(data.getKills())
                : String.format("%.2f", (double) data.getKills() / data.getDeaths());

        String statsMsg = MessageUtil.getMessage("stats",
                        "<gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                                + "<white>Elo: <green>{elo} | Hạng: {rank}\n"
                                + "<white>K/D: <yellow>{kd} ({kills}/{deaths})\n"
                                + "<white>Streak cao nhất: <red>{streak}\n"
                                + "<white>Elo đỉnh: <gold>{peak}")
                .replace("{elo}", String.valueOf(data.getElo()))
                .replace("{rank}", rank + streakPart)
                .replace("{kd}", kd)
                .replace("{kills}", String.valueOf(data.getKills()))
                .replace("{deaths}", String.valueOf(data.getDeaths()))
                .replace("{streak}", String.valueOf(data.getHighestKillStreak()))
                .replace("{peak}", String.valueOf(data.getPeakElo()));

        MessageUtil.sendMessage(sender, statsMsg);

        int totalBounty = data.getBounties().values().stream().mapToInt(Integer::intValue).sum();
        if (totalBounty > 0) {
            MessageUtil.sendMessage(sender, MessageUtil.getMessage("stats-bounty",
                            "<white>Bounty trên đầu: <red>{bounty} elo")
                    .replace("{bounty}", String.valueOf(totalBounty)));
        }

        sendStatsWebhook(sender, data, rank + streakPart, kd, totalBounty);
    }

    private void sendStatsWebhook(CommandSender requester, PlayerData data, String rank, String kd, int totalBounty) {
        if (WebhookManager.getInstance() == null) return;

        java.util.UUID uuid = null;
        try {
            uuid = java.util.UUID.fromString(data.getUUID());
        } catch (IllegalArgumentException ignored) {}

        final java.util.UUID finalUuid = uuid;
        final String name = data.getName();

        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("player",     name);
        replacements.put("elo",        String.valueOf(data.getElo()));
        replacements.put("rank",       MessageUtil.stripTags(rank));
        replacements.put("kd",         kd);
        replacements.put("kills",      String.valueOf(data.getKills()));
        replacements.put("deaths",     String.valueOf(data.getDeaths()));
        replacements.put("streak",     String.valueOf(data.getHighestKillStreak()));
        replacements.put("peak",       String.valueOf(data.getPeakElo()));
        replacements.put("bounty",     String.valueOf(totalBounty));
        replacements.put("requester",  requester.getName());

        replacements.put("avatar_url",    SkinUtil.getAvatarUrl(name, finalUuid));
        replacements.put("texture_url",   SkinUtil.getTextureUrl(name, finalUuid));
        replacements.put("texture_hash",  SkinUtil.extractHash(SkinUtil.getTextureUrl(name, finalUuid)));

        WebhookManager.getInstance().sendStats(replacements);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("gui");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    completions.add(p.getName());
            }
        }
        return completions;
    }
}

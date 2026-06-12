package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.file.module.ProtectionFile;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class NewbieProtectionTask extends BukkitRunnable {

    public NewbieProtectionTask() {
        runTaskTimer(NotKillRank.plugin, 20L, 20L);
    }

    @Override
    public void run() {
        if (!Settings.MODULE_PROTECTION) return;

        FileConfiguration prot = ProtectionFile.get();
        int newbieHours = Settings.PROTECTION_NEWBIE_HOURS;
        int newbieElo   = Settings.PROTECTION_NEWBIE_ELO;

        String formatBoth = prot.getString("actionbar.format-both",
                "<green>⛉ Bảo vệ người mới <dark_gray>| <white>Thời gian: <aqua>{time_left} "
                        + "<dark_gray>| <white>Elo: <yellow>{elo}<white>/<gold>{needed_elo}");
        String formatTimeOnly = prot.getString("actionbar.format-time-only",
                "<green>⛉ Bảo vệ người mới <dark_gray>| <white>Còn: <aqua>{time_left}");
        String formatEloOnly = prot.getString("actionbar.format-elo-only",
                "<green>⛉ Bảo vệ người mới <dark_gray>| <white>Elo: <yellow>{elo}<white>/<gold>{needed_elo}");

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = PluginDataManager.getPlayerDatabase(player.getUniqueId().toString());
            if (data == null) continue;
            if (!EloManager.getInstance().isNewbie(data)) continue;

            long onlineMs    = System.currentTimeMillis() - data.getFirstJoinTime();
            long onlineHours = onlineMs / (60L * 60 * 1000);
            boolean timeProtected = onlineHours < newbieHours;
            boolean eloProtected  = data.getElo() < newbieElo;

            String message;
            if (timeProtected && eloProtected) {
                long remainMs = (newbieHours * 60L * 60 * 1000) - onlineMs;
                message = formatBoth
                        .replace("{time_left}", formatDuration(remainMs))
                        .replace("{elo}", String.valueOf(data.getElo()))
                        .replace("{needed_elo}", String.valueOf(newbieElo));
            } else if (timeProtected) {
                long remainMs = (newbieHours * 60L * 60 * 1000) - onlineMs;
                message = formatTimeOnly.replace("{time_left}", formatDuration(remainMs));
            } else {
                message = formatEloOnly
                        .replace("{elo}", String.valueOf(data.getElo()))
                        .replace("{needed_elo}", String.valueOf(newbieElo));
            }

            Component component = MessageUtil.parse(message);
            player.sendActionBar(component);
        }
    }

    private String formatDuration(long ms) {
        if (ms <= 0) return "0s";
        long totalSeconds = ms / 1000;
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0)   sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("p ");
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}

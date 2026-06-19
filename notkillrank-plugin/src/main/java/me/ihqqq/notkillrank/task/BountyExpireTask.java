package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.manager.BountyManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BountyExpireTask extends BukkitRunnable {

    private static final long INTERVAL_TICKS = 20L * 60 * 5;
    private static BukkitTask currentTask;

    public static void scheduleOrRestart() {
        if (currentTask != null) {
            try { currentTask.cancel(); } catch (Exception ignored) {}
            currentTask = null;
        }
        currentTask = new BountyExpireTask()
                .runTaskTimer(NotKillRank.plugin, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    @Override
    public void run() {
        if (!Settings.MODULE_BOUNTY || Settings.BOUNTY_EXPIRE_HOURS <= 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = PluginDataManager.getPlayerDatabase(player.getUniqueId().toString());
            if (data == null) continue;
            BountyManager.getInstance().expireBounties(data, player.getName());
        }
    }
}

package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AutoSaveTask extends BukkitRunnable {

    private static BukkitTask currentTask;

    public static void scheduleOrRestart() {
        if (currentTask != null) {
            try { currentTask.cancel(); } catch (Exception ignored) {}
            currentTask = null;
        }
        if (!Settings.DATABASE_AUTO_SAVE_ENABLED) return;
        long intervalTicks = 20L * Math.max(30, Settings.DATABASE_AUTO_SAVE_SECONDS);
        currentTask = new AutoSaveTask().runTaskTimerAsynchronously(
                NotKillRank.plugin, intervalTicks, intervalTicks);
    }

    @Override
    public void run() {
        PluginDataManager.saveAllDatabase();
    }
}

package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSaveTask extends BukkitRunnable {

    public AutoSaveTask() {
        if (!Settings.DATABASE_AUTO_SAVE_ENABLED) return;
        long intervalTicks = 20L * Math.max(30, Settings.DATABASE_AUTO_SAVE_SECONDS);
        runTaskTimerAsynchronously(NotKillRank.plugin, intervalTicks, intervalTicks);
    }

    @Override
    public void run() {
        PluginDataManager.saveAllDatabase();
    }
}

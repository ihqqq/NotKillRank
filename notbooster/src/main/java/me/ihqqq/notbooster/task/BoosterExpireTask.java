package me.ihqqq.notbooster.task;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.manager.BoosterManager;
import org.bukkit.Bukkit;

public final class BoosterExpireTask implements Runnable {
    private int taskId = -1;

    public void start() {
        stop();
        long period = Math.max(20L, Settings.CHECK_EXPIRED_INTERVAL_SECONDS * 20L);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(NotBooster.plugin, this, period, period);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        BoosterManager.getInstance().cleanupExpired();
    }
}

package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.file.module.WebhookFile;
import me.ihqqq.notkillrank.webhook.WebhookManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TopEloWebhookTask extends BukkitRunnable {

    private static BukkitTask currentTask;

    public static void scheduleOrRestart() {
        if (currentTask != null) {
            try { currentTask.cancel(); } catch (Exception ignored) {}
            currentTask = null;
        }

        FileConfiguration cfg = WebhookFile.get();
        if (cfg == null) return;
        if (!cfg.getBoolean("top-elo.enabled", false)) return;

        int minutes = cfg.getInt("top-elo.interval-minutes", 60);
        if (minutes <= 0) return;

        long ticks = 20L * 60 * minutes;
        currentTask = new TopEloWebhookTask()
                .runTaskTimerAsynchronously(NotKillRank.plugin, ticks, ticks);
    }

    public static void cancelIfRunning() {
        if (currentTask != null) {
            try { currentTask.cancel(); } catch (Exception ignored) {}
            currentTask = null;
        }
    }

    @Override
    public void run() {
        WebhookManager.getInstance().sendTopElo();
    }
}

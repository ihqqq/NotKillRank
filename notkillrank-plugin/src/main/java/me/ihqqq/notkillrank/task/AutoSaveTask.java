package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.DataManager;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSaveTask extends BukkitRunnable {

    public AutoSaveTask() {
        long intervalTicks = 20L * 60 * 5;
        runTaskTimerAsynchronously(NotKillRank.getInstance(), intervalTicks, intervalTicks);
    }

    @Override
    public void run() {
        DataManager.getInstance().saveAll();
    }
}

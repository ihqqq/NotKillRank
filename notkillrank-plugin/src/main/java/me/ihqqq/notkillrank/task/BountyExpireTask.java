package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.manager.BountyManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BountyExpireTask extends BukkitRunnable {

    public BountyExpireTask() {
        long intervalTicks = 20L * 60 * 5;
        runTaskTimerAsynchronously(NotKillRank.plugin, intervalTicks, intervalTicks);
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

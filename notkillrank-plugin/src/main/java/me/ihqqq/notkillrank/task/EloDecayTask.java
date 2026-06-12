package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.storage.PluginDataStorage;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class EloDecayTask extends BukkitRunnable {

    public EloDecayTask() {
        long intervalTicks = 20L * 60 * 60 * 12;
        runTaskTimerAsynchronously(NotKillRank.plugin, intervalTicks, intervalTicks);
    }

    @Override
    public void run() {
        if (!Settings.MODULE_DECAY) return;

        List<PlayerData> allFromDisk = PluginDataStorage.getAllPlayerData();
        int decayed = 0;

        for (PlayerData diskData : allFromDisk) {
            UUID uuid;
            try {
                uuid = UUID.fromString(diskData.getUUID());
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (Bukkit.getPlayer(uuid) != null) {
                PlayerData cacheData = PluginDataManager.getPlayerDatabase(diskData.getUUID());
                if (cacheData != null) {
                    int before = cacheData.getElo();
                    EloManager.getInstance().applyEloDecay(cacheData);
                    if (cacheData.getElo() < before) decayed++;
                }
                continue;
            }

            int before = diskData.getElo();
            EloManager.getInstance().applyEloDecay(diskData);
            if (diskData.getElo() < before) {
                PluginDataStorage.savePlayerData(diskData.getUUID(), diskData);
                decayed++;
            }
        }

        if (decayed > 0) {
            MessageUtil.log("[EloDecay] Applied decay to " + decayed + " player(s).");
        }
    }
}

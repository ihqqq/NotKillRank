package me.ihqqq.notkillrank.task;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.storage.IDataStorage;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public class EloDecayTask extends BukkitRunnable {

    public EloDecayTask() {
        long intervalTicks = 20L * 60 * 60 * 12;
        runTaskTimerAsynchronously(NotKillRank.getInstance(), intervalTicks, intervalTicks);
    }

    @Override
    public void run() {
        IDataStorage storage = DataManager.getInstance().getStorage();
        List<PlayerData> allFromDisk = storage.loadAll();
        int decayed = 0;

        for (PlayerData diskData : allFromDisk) {
            UUID uuid;
            try {
                uuid = UUID.fromString(diskData.getUUID());
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (Bukkit.getPlayer(uuid) != null) {
                PlayerData cacheData = DataManager.getInstance().get(diskData.getUUID());
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
                storage.save(diskData);
                decayed++;
            }
        }

        if (decayed > 0) {
            MessageUtil.log("&7[EloDecay] Applied decay to " + decayed + " player(s).");
        }
    }
}
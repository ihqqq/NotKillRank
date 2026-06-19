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
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class EloDecayTask extends BukkitRunnable {

    private static final long INTERVAL_TICKS = 20L * 60 * 60 * 12;
    private static BukkitTask currentTask;

    public static void scheduleOrRestart() {
        if (currentTask != null) {
            try { currentTask.cancel(); } catch (Exception ignored) {}
            currentTask = null;
        }
        currentTask = new EloDecayTask()
                .runTaskTimerAsynchronously(NotKillRank.plugin, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    @Override
    public void run() {
        if (!Settings.MODULE_DECAY) return;

        List<PlayerData> allFromDisk = PluginDataStorage.getAllPlayerData();

        List<String> onlineUuids = new ArrayList<>();
        List<PlayerData> offlinePlayers = new ArrayList<>();

        for (PlayerData diskData : allFromDisk) {
            UUID uuid;
            try {
                uuid = UUID.fromString(diskData.getUUID());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (Bukkit.getPlayer(uuid) != null) {
                onlineUuids.add(diskData.getUUID());
            } else {
                offlinePlayers.add(diskData);
            }
        }

        AtomicInteger decayed = new AtomicInteger(0);

        if (!onlineUuids.isEmpty()) {
            Bukkit.getScheduler().runTask(NotKillRank.plugin, () -> {
                for (String uuid : onlineUuids) {
                    PlayerData cacheData = PluginDataManager.getPlayerDatabase(uuid);
                    if (cacheData == null) continue;
                    int before = cacheData.getElo();
                    EloManager.getInstance().applyEloDecay(cacheData);
                    if (cacheData.getElo() < before) decayed.incrementAndGet();
                }
            });
        }

        for (PlayerData diskData : offlinePlayers) {
            int before = diskData.getElo();
            EloManager.getInstance().applyEloDecay(diskData);
            if (diskData.getElo() < before) {
                PluginDataStorage.savePlayerData(diskData.getUUID(), diskData);
                decayed.incrementAndGet();
            }
        }

        Bukkit.getScheduler().runTaskLater(NotKillRank.plugin, () -> {
            if (decayed.get() > 0) {
                MessageUtil.log("[EloDecay] Applied decay to " + decayed.get() + " player(s).");
            }
        }, 2L);
    }
}

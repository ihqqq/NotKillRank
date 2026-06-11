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
        List<PlayerData> allPlayers = storage.loadAll();
        int decayed = 0;
        for (PlayerData data : allPlayers) {
            if (Bukkit.getPlayer(UUID.fromString(data.getUUID())) != null) {
                continue;
            }
            int before = data.getElo();
            EloManager.getInstance().applyEloDecay(data);
            if (data.getElo() < before) {
                storage.save(data);
                decayed++;
            }
        }
        if (decayed > 0) {
            MessageUtil.log("&7[EloDecay] Applied decay to " + decayed + " offline player(s).");
        }
    }
}

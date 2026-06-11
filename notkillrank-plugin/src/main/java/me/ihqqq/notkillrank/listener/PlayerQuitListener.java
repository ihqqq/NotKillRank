package me.ihqqq.notkillrank.listener;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    public PlayerQuitListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.getInstance());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        PlayerData data = DataManager.getInstance().get(uuid);
        if (data == null) return;

        long sessionMs = System.currentTimeMillis() - data.getSessionStart();
        data.setDailyOnlineMs(data.getDailyOnlineMs() + sessionMs);
        data.setLastOnline(System.currentTimeMillis());

        PlayerData snapshot = data.snapshot();

        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.getInstance(), () -> {
            DataManager.getInstance().getStorage().save(snapshot);
            DataManager.getInstance().evict(uuid);
        });
    }
}
package me.ihqqq.notkillrank.listener;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.storage.PluginDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    public PlayerQuitListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.plugin);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        PlayerData data = PluginDataManager.getPlayerDatabase(uuid);
        if (data == null) return;

        long sessionMs = System.currentTimeMillis() - data.getSessionStart();
        data.setDailyOnlineMs(data.getDailyOnlineMs() + sessionMs);
        data.setLastOnline(System.currentTimeMillis());

        PlayerData snapshot = data.snapshot();

        PluginDataManager.evictPlayerDatabase(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () ->
                PluginDataStorage.savePlayerData(snapshot.getUUID(), snapshot));
    }
}

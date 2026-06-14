package me.ihqqq.notkillrank.listener;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
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

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("NPC")) return;

        String uuid = player.getUniqueId().toString();

        PlayerData data = PluginDataManager.getPlayerDatabase(uuid);
        if (data == null) return;

        long sessionStart = data.getSessionStart();
        long sessionMs = sessionStart > 0 ? System.currentTimeMillis() - sessionStart : 0;
        data.setDailyOnlineMs(data.getDailyOnlineMs() + sessionMs);
        data.setLastOnline(System.currentTimeMillis());

        if (Settings.MODULE_STREAKS && Settings.STREAKS_RESET_ON_LOGOUT) {
            data.setKillStreak(0);
            data.setDeathStreak(0);
        }

        PlayerData snapshot = data.snapshot();

        PluginDataManager.evictPlayerDatabase(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () ->
                PluginDataStorage.savePlayerData(snapshot.getUUID(), snapshot));
    }
}

package me.ihqqq.notkillrank.listener;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.api.event.NKRPlayerDataLoadedEvent;
import me.ihqqq.notkillrank.manager.BountyManager;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.LocalDate;

public class PlayerJoinListener implements Listener {

    public PlayerJoinListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("NPC")) return;

        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            PlayerData data = PluginDataManager.getOrCreate(player);

            boolean firstJoin = (System.currentTimeMillis() - data.getFirstJoinTime()) < 10_000L
                    && data.getKills() == 0 && data.getDeaths() == 0;

            EloManager.getInstance().applyEloDecay(data);

            if (Settings.MODULE_BOUNTY) {
                BountyManager.getInstance().expireBounties(data, player.getName());
            }

            String today = LocalDate.now().toString();
            if (!today.equals(data.getCurrentDay())) {
                data.setCurrentDay(today);
                data.setDailyOnlineMs(0);
            }

            data.setSessionStart(System.currentTimeMillis());
            data.setLastOnline(System.currentTimeMillis());

            PluginDataManager.savePlayerDatabaseToStorage(player.getUniqueId().toString());

            final boolean isFirstJoin = firstJoin;
            Bukkit.getScheduler().runTask(NotKillRank.plugin, () -> {
                if (!player.isOnline()) return;

                Bukkit.getPluginManager().callEvent(new NKRPlayerDataLoadedEvent(player, data, isFirstJoin));

                String rank      = RankManager.getInstance().getRankTag(data.getElo());
                String streakTag = RankManager.getInstance().getStreakTag(data);
                String streakPart = streakTag.isEmpty() ? "" : " " + streakTag;
                MessageUtil.sendMessage(player,
                        MessageUtil.getPrefix()
                                + "<white>Chào mừng <yellow>" + player.getName()
                                + " <white>| Hạng: " + rank + streakPart
                                + " <white>| Elo: <green>" + data.getElo());
            });
        });
    }
}

package me.ihqqq.notkillrank.listener;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.EloManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatListener implements Listener {

    public CombatListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.getInstance());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) return;

        EloManager.getInstance().processKill(killer, victim);
    }
}

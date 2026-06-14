package me.ihqqq.notkillrank.listener;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.hook.PvPManagerHook;
import me.ihqqq.notkillrank.manager.EloManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class CombatListener implements Listener {

    public CombatListener() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) return;

        if (victim.hasMetadata("NPC")) return;

        if (Settings.MODULE_PVPMANAGER) {
            PvPManagerHook hook = PvPManagerHook.getInstance();
            if (hook != null) {
                if (hook.shouldSkipElo(victim)) return;
                if (hook.isCombatLogging(victim)) return;
            }
        }

        EloManager.getInstance().processKill(killer, victim);
    }
}

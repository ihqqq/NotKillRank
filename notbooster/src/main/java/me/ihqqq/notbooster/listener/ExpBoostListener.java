package me.ihqqq.notbooster.listener;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.booster.BoosterType;
import me.ihqqq.notbooster.manager.BoosterManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public final class ExpBoostListener implements Listener {

    public ExpBoostListener() {
        Bukkit.getPluginManager().registerEvents(this, NotBooster.plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExp(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) return;
        double multiplier = BoosterManager.getInstance().resolveMultiplier(
                event.getPlayer().getUniqueId(), BoosterType.EXP, Settings.getMultiplierCap(BoosterType.EXP));
        if (multiplier <= 1.0D) return;
        event.setAmount((int) Math.round(event.getAmount() * multiplier));
    }
}

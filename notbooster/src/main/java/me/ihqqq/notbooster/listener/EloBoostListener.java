package me.ihqqq.notbooster.listener;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.booster.BoosterType;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notkillrank.api.event.EloChangeReason;
import me.ihqqq.notkillrank.api.event.NKREloChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class EloBoostListener implements Listener {

    public EloBoostListener() {
        Bukkit.getPluginManager().registerEvents(this, NotBooster.plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEloChange(NKREloChangeEvent event) {
        if (!getAllowedReasons().contains(event.getReason())) return;
        int delta = event.getDelta();
        if (Settings.ELO_BOOST_ONLY_POSITIVE_DELTA && delta <= 0) return;

        double multiplier = BoosterManager.getInstance().resolveMultiplier(
                event.getPlayerUuid(), BoosterType.ELO, Settings.getMultiplierCap(BoosterType.ELO));
        if (multiplier <= 1.0D) return;

        int boostedDelta = (int) Math.round(delta * multiplier);
        event.setNewElo(event.getOldElo() + boostedDelta);
    }

    private Set<EloChangeReason> getAllowedReasons() {
        Set<EloChangeReason> reasons = new HashSet<>();
        for (String raw : Settings.ELO_APPLY_REASONS) {
            reasons.add(EloChangeReason.valueOf(raw.toUpperCase(Locale.ROOT)));
        }
        if (reasons.isEmpty()) reasons.add(EloChangeReason.KILL);
        return reasons;
    }
}

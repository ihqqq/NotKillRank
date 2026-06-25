package me.ihqqq.notbooster.support;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.booster.BoosterType;
import me.ihqqq.notbooster.manager.BoosterManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class PlaceholderAPISupport extends PlaceholderExpansion {
    @Override public @NotNull String getIdentifier() { return "notbooster"; }
    @Override public @NotNull String getAuthor() { return "ihqqq"; }
    @Override public @NotNull String getVersion() { return "1.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        BoosterType type = BoosterType.parse(params.toLowerCase(Locale.ROOT).replace("_multiplier", "")).orElse(null);
        if (type == null) return "";
        return String.format(Locale.US, "%.2f", BoosterManager.getInstance().resolveMultiplier(player.getUniqueId(), type, Settings.getMultiplierCap(type)));
    }
}

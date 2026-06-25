package me.ihqqq.notbooster;

import me.ihqqq.notbooster.booster.BoosterType;
import me.ihqqq.notbooster.booster.StackingMode;
import me.ihqqq.notbooster.booster.TimeParser;
import me.ihqqq.notbooster.enums.StorageType;
import me.ihqqq.notbooster.file.ConfigFile;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public final class Settings {

    public static String LOCALE;
    public static int CHECK_EXPIRED_INTERVAL_SECONDS;
    public static int MAX_PERSONAL_BOOSTERS_PER_PLAYER;
    public static int MAX_GLOBAL_BOOSTERS;
    public static long MAX_DURATION_PERSONAL_MS;
    public static long MAX_DURATION_GLOBAL_MS;
    public static StackingMode STACKING_MODE;
    public static boolean ELO_BOOST_ONLY_POSITIVE_DELTA;
    public static List<String> ELO_APPLY_REASONS;
    public static boolean CATAMINES_ENABLED;
    public static String CATAMINES_REWARD_MODE;
    public static boolean PLACEHOLDERAPI_ENABLED;
    public static boolean GUI_ENABLED;
    public static StorageType STORAGE_TYPE;

    private Settings() {}

    public static void setupValue() {
        FileConfiguration config = ConfigFile.get();
        LOCALE = config.getString("settings.locale", "vi");
        CHECK_EXPIRED_INTERVAL_SECONDS = config.getInt("settings.check-expired-interval-seconds", 30);
        MAX_PERSONAL_BOOSTERS_PER_PLAYER = config.getInt("limits.max-personal-boosters-per-player", 3);
        MAX_GLOBAL_BOOSTERS = config.getInt("limits.max-global-boosters", 5);
        MAX_DURATION_PERSONAL_MS = TimeParser.parseMillis(config.getString("limits.max-duration.personal", "24h"));
        MAX_DURATION_GLOBAL_MS = TimeParser.parseMillis(config.getString("limits.max-duration.global", "6h"));
        STACKING_MODE = StackingMode.parse(config.getString("stacking.mode"));
        ELO_BOOST_ONLY_POSITIVE_DELTA = config.getBoolean("elo.boost-only-positive-delta", true);
        ELO_APPLY_REASONS = config.getStringList("elo.apply-reasons");
        CATAMINES_ENABLED = config.getBoolean("catamines.enabled", true);
        CATAMINES_REWARD_MODE = config.getString("catamines.reward-mode", "BLOCK_DROP");
        PLACEHOLDERAPI_ENABLED = config.getBoolean("placeholderapi.enabled", true);
        GUI_ENABLED = config.getBoolean("gui.enabled", true);
        STORAGE_TYPE = StorageType.parse(config.getString("storage.type", "YAML"));
    }

    public static double getMultiplierCap(BoosterType type) {
        FileConfiguration config = ConfigFile.get();
        return config.getDouble("stacking.cap-effective-multiplier." + type.getConfigKey(),
                config.getDouble("limits.max-multiplier." + type.getConfigKey(), 5.0D));
    }

    public static double getMaxMultiplier(BoosterType type) {
        return ConfigFile.get().getDouble("limits.max-multiplier." + type.getConfigKey(), 5.0D);
    }

    public static boolean hasEffectPreset(String preset) {
        return ConfigFile.get().isConfigurationSection("effect-presets." + preset);
    }
}

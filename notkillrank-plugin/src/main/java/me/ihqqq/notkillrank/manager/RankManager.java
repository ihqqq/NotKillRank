package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.file.module.RanksFile;
import me.ihqqq.notkillrank.file.module.StreaksFile;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RankManager {

    private static RankManager instance;
    private volatile List<RankTier> tiers = new ArrayList<>();
    private volatile RankUpConfig defaults = RankUpConfig.fallback();

    public RankManager() {
        instance = this;
        reload();
    }

    public static RankManager getInstance() {
        return instance;
    }

    public static void reload() {
        if (instance == null) return;
        FileConfiguration cfg = RanksFile.get();

        RankUpConfig base = parseDefaults(cfg);
        instance.defaults = base;

        List<?> rankList = cfg.getList("ranks");
        List<RankTier> newTiers = new ArrayList<>();
        if (rankList != null) {
            for (Object obj : rankList) {
                if (obj instanceof Map<?, ?> rawMap) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) rawMap;
                    int min = toInt(map.getOrDefault("min", 0));
                    int max = toInt(map.getOrDefault("max", 999));
                    String tag = String.valueOf(map.getOrDefault("tag", "<gray>[?]"));
                    RankUpConfig rankup = parseRankupOverride(map, base);
                    newTiers.add(new RankTier(min, max, tag, rankup));
                }
            }
        }
        instance.tiers = newTiers;
    }

    public String getRankTag(int elo) {
        List<RankTier> snapshot = tiers;
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            RankTier tier = snapshot.get(i);
            if (elo >= tier.min) return tier.tag;
        }
        return snapshot.isEmpty() ? "" : snapshot.get(0).tag;
    }

    public void checkRankUp(Player player, PlayerData data, int oldElo, int newElo) {
        if (newElo <= oldElo) return;

        String oldTag = getRankTag(oldElo);
        String newTag = getRankTag(newElo);
        if (oldTag.equals(newTag)) return;

        RankTier newTier = getTierFor(newElo);
        if (newTier == null || newTier.rankup == null) return;

        RankUpConfig cfg = newTier.rankup;
        String rankStripped = MiniMessage.miniMessage().stripTags(newTag);

        String playerName = player.getName();

        if (cfg.messageEnabled && cfg.messageText != null && !cfg.messageText.isEmpty()) {
            String text = applyPlaceholders(cfg.messageText, playerName, newTag, oldTag, newElo);
            MessageUtil.sendMessage(player, text);
        }

        if (cfg.actionbarEnabled && cfg.actionbarText != null && !cfg.actionbarText.isEmpty()) {
            String text = applyPlaceholders(cfg.actionbarText, playerName, newTag, oldTag, newElo);
            player.sendActionBar(MessageUtil.parse(text));
        }

        if (cfg.titleEnabled) {
            String titleStr    = applyPlaceholders(cfg.titleText, playerName, newTag, oldTag, newElo);
            String subtitleStr = applyPlaceholders(cfg.subtitleText, playerName, newTag, oldTag, newElo);
            Title title = Title.title(
                    MessageUtil.parse(titleStr),
                    MessageUtil.parse(subtitleStr),
                    Title.Times.times(
                            Duration.ofMillis(cfg.titleFadeIn),
                            Duration.ofMillis(cfg.titleStay),
                            Duration.ofMillis(cfg.titleFadeOut)
                    )
            );
            player.showTitle(title);
        }

        if (cfg.broadcastEnabled && cfg.broadcastText != null && !cfg.broadcastText.isEmpty()) {
            String text = applyPlaceholders(cfg.broadcastText, playerName, newTag, oldTag, newElo);
            MessageUtil.sendBroadcast(text);
        }

        if (cfg.soundEnabled) {
            Sound sound = parseSound(cfg.soundName);
            player.playSound(player.getLocation(), sound, cfg.soundVolume, cfg.soundPitch);
        }
    }

    public String getStreakTag(PlayerData data) {
        if (Settings.MODULE_STREAKS) {
            if (data.getKillStreak() >= 10) return "<red>[Sát thần " + data.getKillStreak() + "x]";
            if (isSongSot(data)) return "<green>[Kẻ sống sót]";
            if (isWeak(data)) return "<red>[Kẻ yếu]";
        }
        return "";
    }

    public boolean isSongSot(PlayerData data) {
        long noDeathMs = System.currentTimeMillis() - data.getNoDeathStart();
        boolean notKilledIn24h = noDeathMs >= 24L * 60 * 60 * 1000;
        long sessionMs = data.getSessionStart() > 0
                ? System.currentTimeMillis() - data.getSessionStart() : 0;
        long effectiveDailyMs = data.getDailyOnlineMs() + sessionMs;
        boolean onlineEnough = effectiveDailyMs >= 8L * 60 * 60 * 1000;
        return notKilledIn24h && onlineEnough;
    }

    public boolean isWeak(PlayerData data) {
        if (!Settings.MODULE_STREAKS) return false;
        int threshold = StreaksFile.get().getInt("death-streak.threshold", 3);
        return data.getDeathStreak() >= threshold;
    }

    public List<RankTier> getTiers() {
        return tiers;
    }

    private RankTier getTierFor(int elo) {
        List<RankTier> snapshot = tiers;
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            if (elo >= snapshot.get(i).min) return snapshot.get(i);
        }
        return snapshot.isEmpty() ? null : snapshot.get(0);
    }

    private static String applyPlaceholders(String text,
                                            String player, String rank,
                                            String oldRank, int elo) {
        if (text == null) return "";
        return text
                .replace("{player}", player)
                .replace("{rank}", rank)
                .replace("{old_rank}", oldRank)
                .replace("{elo}", String.valueOf(elo));
    }

    private static Sound parseSound(String name) {
        if (name == null || name.isBlank()) return Sound.ENTITY_PLAYER_LEVELUP;
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sound.ENTITY_PLAYER_LEVELUP;
        }
    }


    private static RankUpConfig parseDefaults(FileConfiguration cfg) {
        ConfigurationSection sec = cfg.getConfigurationSection("rankup-defaults");
        if (sec == null) return RankUpConfig.fallback();

        return new RankUpConfig(
                sec.getBoolean("message-enabled", true),
                sec.getString("message-text",
                        "<gold>✦ <white>Chúc mừng <yellow>{player}<white>! Bạn vừa lên hạng {rank}<white>!"),
                sec.getBoolean("actionbar-enabled", true),
                sec.getString("actionbar-text",
                        "<gold><bold>✦ LÊN HẠNG: <reset>{rank} <gold><bold>✦"),
                sec.getBoolean("title-enabled", true),
                sec.getString("title-text", "<gold><bold>LÊN HẠNG!"),
                sec.getString("subtitle-text", "<white>Hạng mới: {rank}"),
                sec.getInt("title-fade-in-ms", 500),
                sec.getInt("title-stay-ms", 3000),
                sec.getInt("title-fade-out-ms", 1000),
                sec.getBoolean("broadcast-enabled", false),
                sec.getString("broadcast-text",
                        "<gold>✦ <yellow>{player} <white>vừa đạt hạng {rank}<white>!"),
                sec.getBoolean("sound-enabled", true),
                sec.getString("sound-name", "ENTITY_PLAYER_LEVELUP"),
                (float) sec.getDouble("sound-volume", 1.0),
                (float) sec.getDouble("sound-pitch", 1.0)
        );
    }

    @SuppressWarnings("unchecked")
    private static RankUpConfig parseRankupOverride(Map<String, Object> tierMap, RankUpConfig base) {
        Object raw = tierMap.get("rankup");
        if (raw == null) return null;
        if (!(raw instanceof Map)) return null;
        Map<String, Object> ov = (Map<String, Object>) raw;

        return new RankUpConfig(
                getBool(ov, "message-enabled",    base.messageEnabled),
                getString(ov, "message-text",     base.messageText),
                getBool(ov, "actionbar-enabled",  base.actionbarEnabled),
                getString(ov, "actionbar-text",   base.actionbarText),
                getBool(ov, "title-enabled",      base.titleEnabled),
                getString(ov, "title-text",       base.titleText),
                getString(ov, "subtitle-text",    base.subtitleText),
                getInt(ov,   "title-fade-in-ms",  base.titleFadeIn),
                getInt(ov,   "title-stay-ms",     base.titleStay),
                getInt(ov,   "title-fade-out-ms", base.titleFadeOut),
                getBool(ov, "broadcast-enabled",  base.broadcastEnabled),
                getString(ov, "broadcast-text",   base.broadcastText),
                getBool(ov, "sound-enabled",      base.soundEnabled),
                getString(ov, "sound-name",       base.soundName),
                getFloat(ov, "sound-volume",      base.soundVolume),
                getFloat(ov, "sound-pitch",       base.soundPitch)
        );
    }

    private static boolean getBool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        if (v instanceof Boolean b) return b;
        return def;
    }

    private static String getString(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        if (v instanceof String s) return s;
        return def;
    }

    private static int getInt(Map<String, Object> m, String key, int def) {
        return toInt(m.getOrDefault(key, def));
    }

    private static float getFloat(Map<String, Object> m, String key, float def) {
        Object v = m.get(key);
        if (v instanceof Number n) return n.floatValue();
        return def;
    }

    private static int toInt(Object obj) {
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }

    public static class RankTier {
        public final int min, max;
        public final String tag;
        public final RankUpConfig rankup;

        public RankTier(int min, int max, String tag, RankUpConfig rankup) {
            this.min = min;
            this.max = max;
            this.tag = tag;
            this.rankup = rankup;
        }
    }

    public static class RankUpConfig {
        public final boolean messageEnabled;
        public final String  messageText;
        public final boolean actionbarEnabled;
        public final String  actionbarText;
        public final boolean titleEnabled;
        public final String  titleText;
        public final String  subtitleText;
        public final int     titleFadeIn;
        public final int     titleStay;
        public final int     titleFadeOut;
        public final boolean broadcastEnabled;
        public final String  broadcastText;
        public final boolean soundEnabled;
        public final String  soundName;
        public final float   soundVolume;
        public final float   soundPitch;

        public RankUpConfig(boolean messageEnabled, String messageText,
                            boolean actionbarEnabled, String actionbarText,
                            boolean titleEnabled, String titleText, String subtitleText,
                            int titleFadeIn, int titleStay, int titleFadeOut,
                            boolean broadcastEnabled, String broadcastText,
                            boolean soundEnabled, String soundName,
                            float soundVolume, float soundPitch) {
            this.messageEnabled   = messageEnabled;
            this.messageText      = messageText;
            this.actionbarEnabled = actionbarEnabled;
            this.actionbarText    = actionbarText;
            this.titleEnabled     = titleEnabled;
            this.titleText        = titleText;
            this.subtitleText     = subtitleText;
            this.titleFadeIn      = titleFadeIn;
            this.titleStay        = titleStay;
            this.titleFadeOut     = titleFadeOut;
            this.broadcastEnabled = broadcastEnabled;
            this.broadcastText    = broadcastText;
            this.soundEnabled     = soundEnabled;
            this.soundName        = soundName;
            this.soundVolume      = soundVolume;
            this.soundPitch       = soundPitch;
        }

        public static RankUpConfig fallback() {
            return new RankUpConfig(
                    true,
                    "<gold>✦ <white>Chúc mừng <yellow>{player}<white>! Bạn vừa lên hạng {rank}<white>!",
                    true,
                    "<gold><bold>✦ LÊN HẠNG: <reset>{rank} <gold><bold>✦",
                    true,
                    "<gold><bold>LÊN HẠNG!",
                    "<white>Hạng mới: {rank}",
                    500, 3000, 1000,
                    false,
                    "<gold>✦ <yellow>{player} <white>vừa đạt hạng {rank}<white>!",
                    true,
                    "ENTITY_PLAYER_LEVELUP",
                    1.0f, 1.0f
            );
        }
    }
}

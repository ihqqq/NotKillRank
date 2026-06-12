package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.Settings;
import me.ihqqq.notkillrank.file.module.RanksFile;
import me.ihqqq.notkillrank.file.module.StreaksFile;
import me.ihqqq.notkillrank.storage.PlayerData;

import java.util.ArrayList;
import java.util.List;

public class RankManager {

    private static RankManager instance;
    private final List<RankTier> tiers = new ArrayList<>();

    public RankManager() {
        instance = this;
        reload();
    }

    public static RankManager getInstance() {
        return instance;
    }

    public static void reload() {
        if (instance == null) return;
        instance.tiers.clear();
        List<?> rankList = RanksFile.get().getList("ranks");
        if (rankList == null) return;
        for (Object obj : rankList) {
            if (obj instanceof java.util.Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) rawMap;
                int min = toInt(map.getOrDefault("min", 0));
                int max = toInt(map.getOrDefault("max", 999));
                String tag = String.valueOf(map.getOrDefault("tag", "<gray>[?]"));
                instance.tiers.add(new RankTier(min, max, tag));
            }
        }
    }

    public String getRankTag(int elo) {
        for (int i = tiers.size() - 1; i >= 0; i--) {
            RankTier tier = tiers.get(i);
            if (elo >= tier.min) return tier.tag;
        }
        return tiers.isEmpty() ? "" : tiers.get(0).tag;
    }

    public String getStreakTag(PlayerData data) {
        if (Settings.MODULE_VOSONG && isVoSong(data)) return "<light_purple>[Vô song]";

        if (Settings.MODULE_STREAKS) {
            if (data.getKillStreak() >= 10) return "<red>[Sát thần " + data.getKillStreak() + "x]";
            if (isSongSot(data)) return "<green>[Kẻ sống sót]";
            if (isWeak(data)) return "<red>[Kẻ yếu]";
        }
        return "";
    }

    public boolean isVoSong(PlayerData data) {
        if (!Settings.MODULE_VOSONG) return false;
        if (data.getTop1Since() <= 0) return false;
        long msRequired = (long) Settings.VOSONG_DAYS_REQUIRED * 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - data.getTop1Since()) >= msRequired;
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

    public static class RankTier {
        public final int min, max;
        public final String tag;

        public RankTier(int min, int max, String tag) {
            this.min = min;
            this.max = max;
            this.tag = tag;
        }
    }

    private static int toInt(Object obj) {
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }
}

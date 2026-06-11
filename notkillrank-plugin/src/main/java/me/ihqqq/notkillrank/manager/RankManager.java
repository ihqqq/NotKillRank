package me.ihqqq.notkillrank.manager;

import me.ihqqq.notkillrank.config.ConfigManager;
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

    public void reload() {
        tiers.clear();
        List<?> rankList = ConfigManager.getInstance().getRanksConfig().getList("ranks");
        if (rankList == null) return;
        for (Object obj : rankList) {
            if (obj instanceof java.util.Map<?, ?> rawMap) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) rawMap;
                int min = toInt(map.getOrDefault("min", 0));
                int max = toInt(map.getOrDefault("max", 999));
                String tag = String.valueOf(map.getOrDefault("tag", "<gray>[?]"));
                tiers.add(new RankTier(min, max, tag));
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
        if (isVoSong(data)) return "<light_purple>[Vô song]";
        if (data.getKillStreak() >= 10) return "<red>[Sát thần " + data.getKillStreak() + "x]";
        if (isSongSot(data)) return "<green>[Kẻ sống sót]";
        if (isWeak(data)) return "<red>[Kẻ yếu]";
        return "";
    }

    public boolean isVoSong(PlayerData data) {
        if (data.getTop1Since() <= 0) return false;
        int daysRequired = ConfigManager.getInstance().getVoSongConfig().getInt("days-required", 3);
        long msRequired = (long) daysRequired * 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - data.getTop1Since()) >= msRequired;
    }

    public boolean isSongSot(PlayerData data) {
        long noDeathMs = System.currentTimeMillis() - data.getNoDeathStart();
        boolean notKilledIn24h = noDeathMs >= 24L * 60 * 60 * 1000;
        boolean onlineEnough = data.getDailyOnlineMs() >= 8L * 60 * 60 * 1000;
        return notKilledIn24h && onlineEnough;
    }

    public boolean isWeak(PlayerData data) {
        int threshold = ConfigManager.getInstance().getStreaksConfig()
                .getInt("death-streak.threshold", 3);
        return data.getDeathStreak() >= threshold;
    }

    public List<RankTier> getTiers() {
        return tiers;
    }

    public static class RankTier {
        public final int min;
        public final int max;
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

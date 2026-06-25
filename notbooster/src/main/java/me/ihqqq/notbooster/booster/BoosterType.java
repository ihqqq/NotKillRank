package me.ihqqq.notbooster.booster;

import java.util.Locale;
import java.util.Optional;

public enum BoosterType {
    EXP("exp"),
    ELO("elo"),
    ITEM_GAINED("item_gained"),
    EFFECT("effect");

    private final String configKey;

    BoosterType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static Optional<BoosterType> parse(String input) {
        String normalized = input.toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "exp", "xp" -> Optional.of(EXP);
            case "elo" -> Optional.of(ELO);
            case "item", "items", "drop", "drops", "itemgained", "item_gained" -> Optional.of(ITEM_GAINED);
            case "effect", "effects", "potion" -> Optional.of(EFFECT);
            default -> Optional.empty();
        };
    }
}

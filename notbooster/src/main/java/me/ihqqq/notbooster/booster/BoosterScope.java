package me.ihqqq.notbooster.booster;

import java.util.Locale;
import java.util.Optional;

public enum BoosterScope {
    PERSONAL,
    GLOBAL;

    public static Optional<BoosterScope> parse(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        if (normalized.equals("peronal")) normalized = "personal";
        return switch (normalized) {
            case "personal", "player", "self" -> Optional.of(PERSONAL);
            case "global", "server" -> Optional.of(GLOBAL);
            default -> Optional.empty();
        };
    }
}

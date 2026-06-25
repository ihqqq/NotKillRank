package me.ihqqq.notbooster.booster;

import java.util.Locale;

public enum StackingMode {
    MULTIPLY,
    ADD_BONUS,
    HIGHEST;

    public static StackingMode parse(String input) {
        if (input == null) return MULTIPLY;
        return switch (input.toUpperCase(Locale.ROOT)) {
            case "ADD", "ADDITIVE", "ADD_BONUS" -> ADD_BONUS;
            case "HIGHEST", "MAX" -> HIGHEST;
            default -> MULTIPLY;
        };
    }
}

package me.ihqqq.notbooster.booster;

import java.util.Locale;

public final class PowerParser {

    private PowerParser() {}

    public static double parseMultiplier(String input) {
        String value = input.trim().toLowerCase(Locale.ROOT);
        if (value.endsWith("x")) {
            return Double.parseDouble(value.substring(0, value.length() - 1));
        }
        if (value.endsWith("%")) {
            String percent = value.substring(0, value.length() - 1).replace("+", "");
            return 1.0D + (Double.parseDouble(percent) / 100.0D);
        }
        return Double.parseDouble(value);
    }
}

package me.ihqqq.notbooster.command.sub;

import me.ihqqq.notbooster.booster.TimeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CommandUtil {
    static final List<String> TYPES = List.of("exp", "elo", "item", "item_gained", "effect");
    static final List<String> SCOPES = List.of("personal", "peronal", "global");
    static final List<String> POWER_EXAMPLES = List.of("1.5x", "2x", "+50%", "mining");
    static final List<String> DURATION_EXAMPLES = List.of("15m", "30m", "1h", "1d");

    private CommandUtil() {}

    static List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        }
        return result;
    }

    static String formatPower(me.ihqqq.notbooster.booster.BoosterType type, String effectPreset, double multiplier) {
        return type == me.ihqqq.notbooster.booster.BoosterType.EFFECT
                ? effectPreset : String.format(Locale.US, "%.2fx", multiplier);
    }

    static String remaining(long duration) {
        return TimeParser.formatRemaining(duration);
    }
}

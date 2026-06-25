package me.ihqqq.notbooster.booster;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {

    private static final Pattern PART_PATTERN = Pattern.compile("(\\d+)([smhd])");

    private TimeParser() {}

    public static long parseMillis(String input) {
        String value = input.trim().toLowerCase(Locale.ROOT);
        Matcher matcher = PART_PATTERN.matcher(value);
        long total = 0L;
        int matchedChars = 0;
        while (matcher.find()) {
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            matchedChars += matcher.group(0).length();
            total += switch (unit) {
                case "s" -> amount * 1000L;
                case "m" -> amount * 60_000L;
                case "h" -> amount * 3_600_000L;
                case "d" -> amount * 86_400_000L;
                default -> 0L;
            };
        }
        if (total <= 0L || matchedChars != value.length()) {
            throw new IllegalArgumentException("Invalid duration: " + input);
        }
        return total;
    }

    public static String formatRemaining(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        if (days > 0) return days + "d" + hours + "h";
        if (hours > 0) return hours + "h" + minutes + "m";
        if (minutes > 0) return minutes + "m" + seconds + "s";
        return seconds + "s";
    }
}

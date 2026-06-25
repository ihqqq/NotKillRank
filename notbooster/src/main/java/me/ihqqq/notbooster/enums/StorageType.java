package me.ihqqq.notbooster.enums;

import java.util.Locale;

public enum StorageType {
    YAML,
    SQLITE,
    MYSQL;

    public static StorageType parse(String raw) {
        if (raw == null) return YAML;
        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "SQLITE" -> SQLITE;
            case "MYSQL" -> MYSQL;
            default -> YAML;
        };
    }
}

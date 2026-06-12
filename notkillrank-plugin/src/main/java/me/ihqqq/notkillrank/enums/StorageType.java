package me.ihqqq.notkillrank.enums;

public enum StorageType {
    YAML, SQLITE, H2;

    public static StorageType fromString(String value) {
        if (value == null) return YAML;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return YAML;
        }
    }
}

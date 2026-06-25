package me.ihqqq.notbooster.booster;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public final class Booster {

    private final UUID id;
    private final BoosterType type;
    private final BoosterScope scope;
    private final UUID ownerUuid;
    private final String ownerName;
    private final double multiplier;
    private final long startedAt;
    private final long expiresAt;
    private final String effectPreset;
    private final BoosterSourceType sourceType;
    private final String sourceName;

    public boolean isExpired(long now) {
        return expiresAt <= now;
    }

    public long getRemainingMillis(long now) {
        return Math.max(0L, expiresAt - now);
    }
}

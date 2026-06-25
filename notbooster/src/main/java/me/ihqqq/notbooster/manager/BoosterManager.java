package me.ihqqq.notbooster.manager;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.booster.Booster;
import me.ihqqq.notbooster.booster.BoosterScope;
import me.ihqqq.notbooster.booster.BoosterSourceType;
import me.ihqqq.notbooster.booster.BoosterType;
import me.ihqqq.notbooster.booster.StackingMode;
import me.ihqqq.notbooster.storage.BoosterStorage;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BoosterManager {

    private static BoosterManager instance;

    private final BoosterStorage storage;
    private final List<Booster> boosters = new ArrayList<>();
    private StackingMode stackingMode = StackingMode.MULTIPLY;
    private int pendingSaveTask = -1;

    public BoosterManager(BoosterStorage storage) {
        instance = this;
        this.storage = storage;
    }

    public static BoosterManager getInstance() {
        return instance;
    }

    public void load() {
        boosters.clear();
        boosters.addAll(storage.load());
        cleanupExpired();
    }

    public void save() {
        storage.save(boosters);
    }

    public void saveAsyncDebounced() {
        if (pendingSaveTask != -1) return;
        pendingSaveTask = Bukkit.getScheduler().scheduleSyncDelayedTask(NotBooster.plugin, () -> {
            pendingSaveTask = -1;
            List<Booster> snapshot = new ArrayList<>(boosters);
            Bukkit.getScheduler().runTaskAsynchronously(NotBooster.plugin, () -> storage.save(snapshot));
        }, 40L);
    }

    public void flushSave() {
        if (pendingSaveTask != -1) {
            Bukkit.getScheduler().cancelTask(pendingSaveTask);
            pendingSaveTask = -1;
        }
        save();
    }

    public void setStackingMode(StackingMode stackingMode) {
        this.stackingMode = stackingMode;
    }

    public Booster add(BoosterType type, BoosterScope scope, UUID ownerUuid, String ownerName,
                       double multiplier, long durationMillis, String effectPreset,
                       BoosterSourceType sourceType, String sourceName) {
        validateAdd(type, scope, ownerUuid, multiplier, effectPreset);
        long now = System.currentTimeMillis();
        Booster booster = new Booster(UUID.randomUUID(), type, scope, ownerUuid, ownerName, multiplier,
                now, now + durationMillis, effectPreset == null ? "" : effectPreset, sourceType, sourceName);
        boosters.add(booster);
        saveAsyncDebounced();
        return booster;
    }

    public boolean remove(UUID id) {
        boolean removed = boosters.removeIf(booster -> booster.getId().equals(id));
        if (removed) saveAsyncDebounced();
        return removed;
    }

    public int cleanupExpired() {
        long now = System.currentTimeMillis();
        int removed = 0;
        Iterator<Booster> iterator = boosters.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired(now)) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) saveAsyncDebounced();
        return removed;
    }

    public double resolveMultiplier(UUID playerUuid, BoosterType type, double cap) {
        cleanupExpired();
        List<Double> multipliers = getApplicable(playerUuid, type).stream()
                .map(Booster::getMultiplier)
                .toList();
        if (multipliers.isEmpty()) return 1.0D;

        double value = switch (stackingMode) {
            case MULTIPLY -> multipliers.stream().reduce(1.0D, (left, right) -> left * right);
            case ADD_BONUS -> 1.0D + multipliers.stream().mapToDouble(multiplier -> multiplier - 1.0D).sum();
            case HIGHEST -> multipliers.stream().mapToDouble(Double::doubleValue).max().orElse(1.0D);
        };
        return Math.max(1.0D, Math.min(value, cap));
    }

    public List<Booster> getApplicable(UUID playerUuid, BoosterType type) {
        long now = System.currentTimeMillis();
        return boosters.stream()
                .filter(booster -> !booster.isExpired(now))
                .filter(booster -> booster.getType() == type)
                .filter(booster -> booster.getScope() == BoosterScope.GLOBAL
                        || (playerUuid != null && playerUuid.equals(booster.getOwnerUuid())))
                .sorted(Comparator.comparing(Booster::getExpiresAt))
                .toList();
    }

    public List<Booster> getActiveBoosters() {
        cleanupExpired();
        return boosters.stream()
                .sorted(Comparator.comparing(Booster::getExpiresAt))
                .toList();
    }

    public Collection<Booster> rawBoosters() {
        return boosters;
    }

    public Optional<Booster> find(UUID id) {
        return boosters.stream().filter(booster -> booster.getId().equals(id)).findFirst();
    }

    private void validateAdd(BoosterType type, BoosterScope scope, UUID ownerUuid, double multiplier, String effectPreset) {
        if (multiplier < 1.0D) throw new IllegalArgumentException("multiplier-too-low");
        if (type == BoosterType.EFFECT && !Settings.hasEffectPreset(effectPreset)) {
            throw new IllegalArgumentException("effect-preset-not-found");
        }
        long activeCount = boosters.stream()
                .filter(booster -> !booster.isExpired(System.currentTimeMillis()))
                .filter(booster -> booster.getScope() == scope)
                .filter(booster -> scope == BoosterScope.GLOBAL || ownerUuid.equals(booster.getOwnerUuid()))
                .count();
        if (scope == BoosterScope.GLOBAL && activeCount >= Settings.MAX_GLOBAL_BOOSTERS) {
            throw new IllegalArgumentException("too-many-global-boosters");
        }
        if (scope == BoosterScope.PERSONAL && activeCount >= Settings.MAX_PERSONAL_BOOSTERS_PER_PLAYER) {
            throw new IllegalArgumentException("too-many-personal-boosters");
        }
    }
}

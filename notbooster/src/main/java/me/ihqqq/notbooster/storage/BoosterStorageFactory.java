package me.ihqqq.notbooster.storage;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.enums.StorageType;

public final class BoosterStorageFactory {

    private BoosterStorageFactory() {}

    public static BoosterStorage create(StorageType type) {
        if (type != StorageType.YAML) {
            NotBooster.plugin.getLogger().warning(type + " storage is not implemented yet. Falling back to YAML.");
        }
        return new YamlBoosterStorage();
    }
}

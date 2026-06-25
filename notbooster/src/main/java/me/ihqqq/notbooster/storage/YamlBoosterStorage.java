package me.ihqqq.notbooster.storage;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.booster.Booster;
import me.ihqqq.notbooster.booster.BoosterScope;
import me.ihqqq.notbooster.booster.BoosterSourceType;
import me.ihqqq.notbooster.booster.BoosterType;
import me.ihqqq.notbooster.file.BoostersFile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class YamlBoosterStorage implements BoosterStorage {

    @Override
    public Collection<Booster> load() {
        if (!BoostersFile.getFile().exists()) return List.of();
        FileConfiguration config = YamlConfiguration.loadConfiguration(BoostersFile.getFile());
        ConfigurationSection section = config.getConfigurationSection("boosters");
        if (section == null) return List.of();

        List<Booster> boosters = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection item = section.getConfigurationSection(key);
            if (item == null) continue;

            BoosterType type = BoosterType.valueOf(item.getString("type", "ELO"));
            BoosterScope scope = BoosterScope.valueOf(item.getString("scope", "PERSONAL"));
            String ownerValue = item.getString("owner-uuid", null);
            UUID ownerUuid = ownerValue == null || ownerValue.isBlank() ? null : UUID.fromString(ownerValue);
            boosters.add(new Booster(
                    UUID.fromString(key),
                    type,
                    scope,
                    ownerUuid,
                    item.getString("owner-name", ""),
                    item.getDouble("multiplier", 1.0D),
                    item.getLong("started-at"),
                    item.getLong("expires-at"),
                    item.getString("effect-preset", ""),
                    BoosterSourceType.valueOf(item.getString("source-type", "STORAGE")),
                    item.getString("source-name", item.getString("source", "STORAGE"))
            ));
        }
        return boosters;
    }

    @Override
    public void save(Collection<Booster> boosters) {
        FileConfiguration config = new YamlConfiguration();
        for (Booster booster : boosters) {
            String path = "boosters." + booster.getId();
            config.set(path + ".type", booster.getType().name());
            config.set(path + ".scope", booster.getScope().name());
            config.set(path + ".owner-uuid", booster.getOwnerUuid() == null ? null : booster.getOwnerUuid().toString());
            config.set(path + ".owner-name", booster.getOwnerName());
            config.set(path + ".multiplier", booster.getMultiplier());
            config.set(path + ".started-at", booster.getStartedAt());
            config.set(path + ".expires-at", booster.getExpiresAt());
            config.set(path + ".effect-preset", booster.getEffectPreset());
            config.set(path + ".source-type", booster.getSourceType().name());
            config.set(path + ".source-name", booster.getSourceName());
        }
        try {
            config.save(BoostersFile.getFile());
        } catch (IOException e) {
            NotBooster.plugin.getLogger().severe("Could not save boosters.yml: " + e.getMessage());
        }
    }
}

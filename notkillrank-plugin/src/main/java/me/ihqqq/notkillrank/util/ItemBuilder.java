package me.ihqqq.notkillrank.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    public static ItemStack fromIconSection(ConfigurationSection icon) {
        if(icon == null) return new ItemStack(Material.STONE);

        String type = icon.getString("type", "STONE").toUpperCase();
        String texture = icon.getString("texture", icon.getString("value", null));

        if(type.equals("CUSTOM_HEAD") || (type.equals("PLAYER_HEAD") && texture != null)) {
            if(texture != null && !texture.isEmpty()) {
                return createTextureHead(texture);
            }
            return new ItemStack(Material.PLAYER_HEAD);
        }

        Material mat;
        try {
            mat = Material.valueOf(type);
        } catch (IllegalArgumentException e) {
            MessageUtil.warn("ItemBuilder: unknown material '" + type + "', falling back to STONE.");
            mat = Material.STONE;
        }
        return new ItemStack(mat);
    }

    public static ItemStack fromIconSection(ConfigurationSection icon,
                                            String displayName,
                                            List<String> lore) {
        ItemStack item = fromIconSection(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (displayName != null) {
            meta.displayName(MessageUtil.parse(displayName));
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> components = new ArrayList<>();
            for (String line : lore) {
                if (line == null || line.isEmpty()) {
                    components.add(Component.empty());
                } else {
                    components.add(MessageUtil.parse(line));
                }
            }
            meta.lore(components);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createTextureHead(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        if(meta == null) return skull;

        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            String url = decoded.split("\"url\":\"")[1].split("\"")[0];

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            skull.setItemMeta(meta);


        } catch (MalformedURLException e) {
            MessageUtil.warn("ItemBuilder: failed to apply base64 texture — " + e.getMessage());
        }
        return skull;
    }
}

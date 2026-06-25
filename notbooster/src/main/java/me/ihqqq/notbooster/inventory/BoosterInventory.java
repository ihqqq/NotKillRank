package me.ihqqq.notbooster.inventory;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.booster.Booster;
import me.ihqqq.notbooster.manager.BoosterManager;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BoosterInventory implements Listener {

    private static final String TITLE = "NotBooster Active Boosters";

    public BoosterInventory() {
        Bukkit.getPluginManager().registerEvents(this, NotBooster.plugin);
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        long now = System.currentTimeMillis();
        int slot = 0;
        for (Booster booster : BoosterManager.getInstance().getActiveBoosters()) {
            if (slot >= inventory.getSize()) break;
            ItemStack item = new ItemStack(booster.getScope().name().equals("GLOBAL") ? Material.EMERALD : Material.DIAMOND);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtil.color("<yellow>" + booster.getType().name() + " <gray>" + booster.getId().toString().substring(0, 8)));
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.color("<gray>ID: <white>" + booster.getId()));
            lore.add(MessageUtil.color("<gray>Owner: <white>" + booster.getOwnerName()));
            lore.add(MessageUtil.color("<gray>Scope: <white>" + booster.getScope().name()));
            lore.add(MessageUtil.color("<gray>Power: <white>" + booster.getMultiplier() + "x"));
            lore.add(MessageUtil.color("<gray>Remaining: <white>" + me.ihqqq.notbooster.booster.TimeParser.formatRemaining(booster.getRemainingMillis(now))));
            lore.add(MessageUtil.color("<red>Shift-click de stop neu co quyen admin"));
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!event.isShiftClick() || !(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("notbooster.admin")) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return;
        String raw = org.bukkit.ChatColor.stripColor(item.getItemMeta().getLore().get(0)).replace("ID: ", "");
        try {
            if (BoosterManager.getInstance().remove(UUID.fromString(raw))) {
                MessageUtil.send(player, "booster-stopped");
                open(player);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid item lore should never happen for this inventory.
        }
    }
}

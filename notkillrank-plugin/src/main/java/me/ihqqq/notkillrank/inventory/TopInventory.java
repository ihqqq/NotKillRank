package me.ihqqq.notkillrank.inventory;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class TopInventory implements Listener {

    private static final String TITLE_MM = "<gold><bold>Top 10 — NotKillRank";
    private static final Component TITLE_COMPONENT = MessageUtil.parse(TITLE_MM);
    private static final String TITLE_PLAIN = PlainTextComponentSerializer.plainText()
            .serialize(TITLE_COMPONENT);

    public TopInventory() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.getInstance());
    }

    public static void open(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.getInstance(), () -> {
            List<PlayerData> top = DataManager.getInstance().getTopPlayers(10);
            Bukkit.getScheduler().runTask(NotKillRank.getInstance(), () -> {
                Inventory inv = Bukkit.createInventory(null, 54, TITLE_COMPONENT);
                fillBorder(inv);
                for (int i = 0; i < Math.min(top.size(), 10); i++) {
                    int slot = getSlotForPos(i + 1);
                    if (slot >= 0) inv.setItem(slot, createSkull(top.get(i), i + 1));
                }
                player.openInventory(inv);
            });
        });
    }

    private static int getSlotForPos(int pos) {
        int[] slots = {13, 11, 15, 20, 24, 29, 33, 28, 30, 22};
        if (pos < 1 || pos > slots.length) return -1;
        return slots[pos - 1];
    }

    private static ItemStack createSkull(PlayerData data, int pos) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        Player online = Bukkit.getPlayer(java.util.UUID.fromString(data.getUUID()));
        if (online != null) {
            meta.setOwningPlayer(online);
        } else {
            meta.setOwner(data.getName());
        }

        String rankTag = RankManager.getInstance().getRankTag(data.getElo());
        String streakTag = RankManager.getInstance().getStreakTag(data);
        String streakPart = streakTag.isEmpty() ? "" : " " + streakTag;

        meta.displayName(MessageUtil.parse(getMedalMM(pos) + " <white>" + data.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(MessageUtil.parse("<gray>Hạng: " + rankTag + streakPart));
        lore.add(MessageUtil.parse("<gray>Elo: <green>" + data.getElo()));
        lore.add(MessageUtil.parse("<gray>Kill/Death: <yellow>" + data.getKills()
                + "<gray>/<red>" + data.getDeaths()));
        lore.add(MessageUtil.parse("<gray>Kill streak cao nhất: <red>" + data.getHighestKillStreak()));
        lore.add(MessageUtil.parse("<gray>Peak elo: <gold>" + data.getPeakElo()));

        int totalBounty = data.getBounties().values().stream().mapToInt(Integer::intValue).sum();
        if (totalBounty > 0) {
            lore.add(Component.empty());
            lore.add(MessageUtil.parse("<gold>Bounty: <red>" + totalBounty + " elo"));
        }

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private static String getMedalMM(int pos) {
        return switch (pos) {
            case 1 -> "<gold><bold>#1";
            case 2 -> "<gray><bold>#2";
            case 3 -> "<red><bold>#3";
            default -> "<dark_gray>#" + pos;
        };
    }

    private static void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            glass.setItemMeta(meta);
        }
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
        for (int i = 0; i < 54; i += 9) inv.setItem(i, glass);
        for (int i = 8; i < 54; i += 9) inv.setItem(i, glass);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String plain = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        if (plain.equals(TITLE_PLAIN)) {
            event.setCancelled(true);
        }
    }
}

package me.ihqqq.notkillrank.inventory;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.config.ConfigManager;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.ItemBuilder;
import me.ihqqq.notkillrank.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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

    public TopInventory() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.getInstance());
    }

    public static void open(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.getInstance(), () -> {
            List<PlayerData> top = DataManager.getInstance().getTopPlayers(10);
            Bukkit.getScheduler().runTask(NotKillRank.getInstance(), () -> {
                FileConfiguration gui = ConfigManager.getInstance().getTopGui();

                String titleMM = gui.getString("name", "<gold><bold>Top 10 — NotKillRank");
                Component titleComponent = MessageUtil.parse(titleMM);
                int rows = Math.max(1, Math.min(6, gui.getInt("rows", 6)));
                int size = rows * 9;

                Inventory inv = Bukkit.createInventory(null, size, titleComponent);
                fillBackground(inv, gui, rows);

                List<Integer> slots = gui.getIntegerList("player-slots");
                if (slots.isEmpty()) {
                    slots = List.of(13, 11, 15, 20, 24, 29, 33, 28, 30, 22);
                }

                for (int i = 0; i < Math.min(top.size(), slots.size()); i++) {
                    int slot = slots.get(i);
                    if (slot >= 0 && slot < size) {
                        inv.setItem(slot, createSkull(top.get(i), i + 1, gui));
                    }
                }
                player.openInventory(inv);
            });
        });
    }

    private static void fillBackground(Inventory inv, FileConfiguration gui, int rows) {
        ConfigurationSection iconSection = gui.getConfigurationSection("background.icon");
        ItemStack bgItem = ItemBuilder.fromIconSection(iconSection);

        ItemMeta meta = bgItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            bgItem.setItemMeta(meta);
        }

        List<String> fillPattern = gui.getStringList("background.fill");
        if (fillPattern.isEmpty()) {
            for (int i = 0; i < 9; i++) inv.setItem(i, bgItem);
            for (int i = (rows - 1) * 9; i < rows * 9; i++) inv.setItem(i, bgItem);
            for (int i = 0; i < rows * 9; i += 9) inv.setItem(i, bgItem);
            for (int i = 8; i < rows * 9; i += 9) inv.setItem(i, bgItem);
            return;
        }

        for (int row = 0; row < Math.min(fillPattern.size(), rows); row++) {
            String rowStr = fillPattern.get(row);
            for (int col = 0; col < Math.min(rowStr.length(), 9); col++) {
                if (rowStr.charAt(col) == 'x') {
                    inv.setItem(row * 9 + col, bgItem.clone());
                }
            }
        }
    }

    private static ItemStack createSkull(PlayerData data, int pos, FileConfiguration gui) {
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
        String medal = getMedal(pos, gui);

        String nameTpl = gui.getString("player-head.name", "{medal} <white>{player}");
        String displayName = nameTpl
                .replace("{medal}", medal)
                .replace("{player}", data.getName());
        meta.displayName(MessageUtil.parse(displayName));

        List<Component> lore = new ArrayList<>();
        List<String> loreTpl = gui.getStringList("player-head.lore");
        if (loreTpl.isEmpty()) {
            loreTpl = List.of(
                    "<gray>Hạng: {rank}",
                    "<gray>Elo: <green>{elo}",
                    "<gray>Kill/Death: <yellow>{kills}<gray>/<red>{deaths}",
                    "<gray>Kill streak cao nhất: <red>{highest_streak}",
                    "<gray>Peak elo: <gold>{peak_elo}"
            );
        }

        int totalBounty = data.getBounties().values().stream().mapToInt(Integer::intValue).sum();
        String kd = data.getDeaths() == 0
                ? String.valueOf(data.getKills())
                : String.format("%.2f", (double) data.getKills() / data.getDeaths());

        for (String line : loreTpl) {
            String resolved = line
                    .replace("{rank}", rankTag + streakPart)
                    .replace("{elo}", String.valueOf(data.getElo()))
                    .replace("{kills}", String.valueOf(data.getKills()))
                    .replace("{deaths}", String.valueOf(data.getDeaths()))
                    .replace("{kd}", kd)
                    .replace("{highest_streak}", String.valueOf(data.getHighestKillStreak()))
                    .replace("{peak_elo}", String.valueOf(data.getPeakElo()))
                    .replace("{bounty}", String.valueOf(totalBounty));
            lore.add(MessageUtil.parse(resolved));
        }

        if (totalBounty > 0) {
            List<String> bountyLoreTpl = gui.getStringList("player-head.bounty-lore");
            if (bountyLoreTpl.isEmpty()) {
                bountyLoreTpl = List.of("", "<gold>Bounty: <red>{bounty} elo");
            }
            for (String line : bountyLoreTpl) {
                if (line.isEmpty()) {
                    lore.add(Component.empty());
                } else {
                    lore.add(MessageUtil.parse(line.replace("{bounty}", String.valueOf(totalBounty))));
                }
            }
        }

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private static String getMedal(int pos, FileConfiguration gui) {
        String key = "medals." + pos;
        if (gui.contains(key)) {
            return gui.getString(key, "#" + pos);
        }
        String def = gui.getString("medals.default", "<dark_gray>#{pos}");
        return def.replace("{pos}", String.valueOf(pos));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        FileConfiguration gui = ConfigManager.getInstance().getTopGui();
        String titleMM = gui.getString("name", "<gold><bold>Top 10 — NotKillRank");
        String titlePlain = MessageUtil.stripTags(titleMM);
        String viewPlain = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());
        if (viewPlain.equals(titlePlain)) {
            event.setCancelled(true);
        }
    }
}

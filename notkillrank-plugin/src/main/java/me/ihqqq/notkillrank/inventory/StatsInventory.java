package me.ihqqq.notkillrank.inventory;

import com.destroystokyo.paper.profile.PlayerProfile;
import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.file.module.StatsGuiFile;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.storage.PluginDataManager;
import me.ihqqq.notkillrank.util.ItemBuilder;
import me.ihqqq.notkillrank.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatsInventory implements NotKillRankInventoryBase {

    private static final Map<String, CachedProfile> skinCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    public static final class Holder implements org.bukkit.inventory.InventoryHolder {
        private final int page;
        private final int totalPages;
        public Holder(int page, int totalPages) {
            this.page = page;
            this.totalPages = totalPages;
        }
        public int getPage() { return page; }
        public int getTotalPages() { return totalPages; }
        @Override public Inventory getInventory() { return null; }
    }

    private record CachedProfile(PlayerProfile profile, long fetchedAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS;
        }
    }

    public StatsInventory() {
        Bukkit.getPluginManager().registerEvents(this, NotKillRank.plugin);
    }

    public static void open(Player player, int page) {
        Bukkit.getScheduler().runTaskAsynchronously(NotKillRank.plugin, () -> {
            FileConfiguration gui = StatsGuiFile.get();
            int playersPerPage = resolvePlayersPerPage(gui);

            List<PlayerData> allPlayers = PluginDataManager.getTopPlayers(Integer.MAX_VALUE);
            // Sort by elo desc
            allPlayers.sort((a, b) -> b.getElo() - a.getElo());

            int totalPages = Math.max(1, (int) Math.ceil((double) allPlayers.size() / playersPerPage));
            int safePage = Math.max(0, Math.min(page, totalPages - 1));

            int fromIndex = safePage * playersPerPage;
            int toIndex = Math.min(fromIndex + playersPerPage, allPlayers.size());
            List<PlayerData> pagePlayers = (fromIndex < allPlayers.size())
                    ? allPlayers.subList(fromIndex, toIndex)
                    : Collections.emptyList();

            // Fetch profiles
            Map<String, PlayerProfile> profileMap = new HashMap<>();
            for (PlayerData data : pagePlayers) {
                UUID uuid;
                try { uuid = UUID.fromString(data.getUUID()); }
                catch (IllegalArgumentException ignored) { continue; }

                Player online = Bukkit.getPlayer(uuid);
                if (online != null) {
                    PlayerProfile live = online.getPlayerProfile();
                    profileMap.put(data.getUUID(), live);
                    skinCache.put(data.getUUID(), new CachedProfile(live, System.currentTimeMillis()));
                } else {
                    CachedProfile cached = skinCache.get(data.getUUID());
                    if (cached != null && !cached.isExpired()) {
                        profileMap.put(data.getUUID(), cached.profile());
                        continue;
                    }
                    try {
                        PlayerProfile profile = Bukkit.createProfile(uuid, data.getName());
                        profile.complete(true);
                        skinCache.put(data.getUUID(), new CachedProfile(profile, System.currentTimeMillis()));
                        profileMap.put(data.getUUID(), profile);
                    } catch (Exception e) {
                        try {
                            PlayerProfile fallback = Bukkit.createProfile(uuid, data.getName());
                            fallback.completeFromCache();
                            profileMap.put(data.getUUID(), fallback);
                        } catch (Exception ignored) {
                            profileMap.put(data.getUUID(), null);
                        }
                    }
                }
            }

            final int finalPage = safePage;
            final int finalTotalPages = totalPages;
            final Map<String, PlayerProfile> finalProfileMap = profileMap;
            final List<PlayerData> finalPagePlayers = pagePlayers;
            final int finalFromIndex = fromIndex;

            Bukkit.getScheduler().runTask(NotKillRank.plugin, () -> {
                if (!player.isOnline()) return;

                String titleRaw = gui.getString("name", "<dark_gray>ꜱᴛᴀᴛꜱ <gray>| <white>Trang {page}<gray>/<white>{total}");
                String titleStr = titleRaw
                        .replace("{page}", String.valueOf(finalPage + 1))
                        .replace("{total}", String.valueOf(finalTotalPages));
                Component title = MessageUtil.parse(titleStr);

                int rows = Math.max(1, Math.min(6, gui.getInt("rows", 6)));
                int size = rows * 9;
                Inventory inv = Bukkit.createInventory(new Holder(finalPage, finalTotalPages), size, title);

                fillBackground(inv, gui, rows);

                List<Integer> slots = gui.getIntegerList("player-slots");
                if (slots.isEmpty()) {
                    slots = defaultSlots(rows, size);
                }

                for (int i = 0; i < Math.min(finalPagePlayers.size(), slots.size()); i++) {
                    int slot = slots.get(i);
                    if (slot >= 0 && slot < size) {
                        int globalRank = finalFromIndex + i + 1;
                        inv.setItem(slot, createSkull(
                                finalPagePlayers.get(i),
                                globalRank,
                                gui,
                                finalProfileMap.get(finalPagePlayers.get(i).getUUID())
                        ));
                    }
                }

                // Navigation buttons
                placeNavButtons(inv, gui, size, finalPage, finalTotalPages);

                // Extra buttons
                placeExtraButtons(inv, gui, size);

                player.openInventory(inv);
            });
        });
    }

    // --- helpers ---

    private static int resolvePlayersPerPage(FileConfiguration gui) {
        int v = gui.getInt("players-per-page", 28);
        return Math.max(1, Math.min(v, 45));
    }

    private static List<Integer> defaultSlots(int rows, int size) {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
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
            int size = rows * 9;
            for (int i = 0; i < 9; i++) inv.setItem(i, bgItem.clone());
            for (int i = (rows - 1) * 9; i < size; i++) inv.setItem(i, bgItem.clone());
            for (int i = 0; i < size; i += 9) inv.setItem(i, bgItem.clone());
            for (int i = 8; i < size; i += 9) inv.setItem(i, bgItem.clone());
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

    private static void placeNavButtons(Inventory inv, FileConfiguration gui, int size,
                                        int currentPage, int totalPages) {
        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;

        // Previous button
        ConfigurationSection prevSec = gui.getConfigurationSection("navigation.prev");
        if (prevSec != null && hasPrev) {
            int slot = prevSec.getInt("slot", (int)(Math.floor(size / 9.0) - 1) * 9);
            if (slot >= 0 && slot < size) {
                String name = prevSec.getString("icon.name",
                        "<yellow>◀ Trang trước <dark_gray>(" + currentPage + "/" + totalPages + ")");
                ItemStack item = ItemBuilder.fromIconSection(
                        prevSec.getConfigurationSection("icon"),
                        "<!italic>" + name,
                        null
                );
                inv.setItem(slot, item);
            }
        } else if (hasPrev) {
            // Fallback prev button slot
            int slot = (int)(Math.floor(size / 9.0) - 1) * 9 + 0;
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, buildFallbackNav("<yellow>◀ Trang trước", Material.ARROW));
            }
        }

        // Next button
        ConfigurationSection nextSec = gui.getConfigurationSection("navigation.next");
        if (nextSec != null && hasNext) {
            int slot = nextSec.getInt("slot", size - 1);
            if (slot >= 0 && slot < size) {
                String name = nextSec.getString("icon.name",
                        "<yellow>Trang tiếp ▶ <dark_gray>(" + (currentPage + 2) + "/" + totalPages + ")");
                ItemStack item = ItemBuilder.fromIconSection(
                        nextSec.getConfigurationSection("icon"),
                        "<!italic>" + name,
                        null
                );
                inv.setItem(slot, item);
            }
        } else if (hasNext) {
            int slot = size - 1;
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, buildFallbackNav("<yellow>Trang tiếp ▶", Material.ARROW));
            }
        }

        // Page indicator
        ConfigurationSection pageSec = gui.getConfigurationSection("navigation.page-indicator");
        if (pageSec != null) {
            int slot = pageSec.getInt("slot", size - 9 + 4);
            if (slot >= 0 && slot < size) {
                String rawName = pageSec.getString("icon.name",
                        "<gray>Trang <white>{page}<gray>/<white>{total}");
                String name = rawName
                        .replace("{page}", String.valueOf(currentPage + 1))
                        .replace("{total}", String.valueOf(totalPages));
                ItemStack item = ItemBuilder.fromIconSection(
                        pageSec.getConfigurationSection("icon"),
                        "<!italic>" + name,
                        null
                );
                inv.setItem(slot, item);
            }
        }
    }

    private static ItemStack buildFallbackNav(String name, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MessageUtil.parse("<!italic>" + name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void placeExtraButtons(Inventory inv, FileConfiguration gui, int size) {
        ConfigurationSection buttons = gui.getConfigurationSection("buttons");
        if (buttons == null) return;
        for (String key : buttons.getKeys(false)) {
            ConfigurationSection btn = buttons.getConfigurationSection(key);
            if (btn == null) continue;
            int slot = btn.getInt("slot", -1);
            if (slot < 0 || slot >= size) continue;
            // Don't overwrite nav buttons that are already placed
            if (inv.getItem(slot) != null) continue;
            String rawName = btn.getString("icon.name", null);
            String btnName = rawName != null ? "<!italic>" + rawName : null;
            List<String> btnLore = null;
            if (btn.contains("icon.lore")) {
                btnLore = new ArrayList<>();
                for (String line : btn.getStringList("icon.lore")) {
                    btnLore.add(line.isEmpty() ? line : "<!italic>" + line);
                }
            }
            inv.setItem(slot, ItemBuilder.fromIconSection(
                    btn.getConfigurationSection("icon"), btnName, btnLore));
        }
    }

    private static ItemStack createSkull(PlayerData data, int globalRank,
                                         FileConfiguration gui, PlayerProfile profile) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;
        if (profile != null) meta.setPlayerProfile(profile);

        String rankTag   = RankManager.getInstance().getRankTag(data.getElo());
        String streakTag = RankManager.getInstance().getStreakTag(data);
        String streakPart = streakTag.isEmpty() ? "" : " " + streakTag;
        String medal = getMedal(globalRank, gui);

        String nameTpl = gui.getString("player-head.name", "{medal} <white>{player}");
        String displayName = nameTpl
                .replace("{medal}", medal)
                .replace("{player}", data.getName())
                .replace("{rank}", globalRank + "");
        meta.displayName(MessageUtil.parseLore(displayName));

        List<Component> lore = new ArrayList<>();
        List<String> loreTpl = gui.getStringList("player-head.lore");
        if (loreTpl.isEmpty()) {
            loreTpl = List.of(
                    "<gray>Hạng: {rank}",
                    "<gray>Elo: <green>{elo}",
                    "<gray>K/D: <yellow>{kd}",
                    "<gray>Kill: <white>{kills} <dark_gray>| <gray>Chết: <white>{deaths}",
                    "<gray>Chuỗi kill cao nhất: <red>{highest_streak}",
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
                    .replace("{pos}", String.valueOf(globalRank))
                    .replace("{elo}", String.valueOf(data.getElo()))
                    .replace("{kills}", String.valueOf(data.getKills()))
                    .replace("{deaths}", String.valueOf(data.getDeaths()))
                    .replace("{kd}", kd)
                    .replace("{highest_streak}", String.valueOf(data.getHighestKillStreak()))
                    .replace("{peak_elo}", String.valueOf(data.getPeakElo()))
                    .replace("{bounty}", String.valueOf(totalBounty))
                    .replace("{kill_streak}", String.valueOf(data.getKillStreak()))
                    .replace("{death_streak}", String.valueOf(data.getDeathStreak()));
            lore.add(MessageUtil.parseLore(resolved));
        }

        if (totalBounty > 0) {
            List<String> bountyLoreTpl = gui.getStringList("player-head.bounty-lore");
            if (bountyLoreTpl.isEmpty()) {
                bountyLoreTpl = List.of("", "<gold>Bounty: <red>{bounty} elo");
            }
            for (String line : bountyLoreTpl) {
                if (line.isEmpty()) lore.add(Component.empty());
                else lore.add(MessageUtil.parseLore(line.replace("{bounty}", String.valueOf(totalBounty))));
            }
        }

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private static String getMedal(int pos, FileConfiguration gui) {
        String key = "medals." + pos;
        if (gui.contains(key)) return gui.getString(key, "#" + pos);
        String def = gui.getString("medals.default", "<dark_gray>#{pos}");
        return def.replace("{pos}", String.valueOf(pos));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        int slot = event.getRawSlot();
        int currentPage = holder.getPage();
        FileConfiguration gui = StatsGuiFile.get();
        int size = event.getInventory().getSize();

        // Check prev button
        ConfigurationSection prevSec = gui.getConfigurationSection("navigation.prev");
        int prevSlot = prevSec != null ? prevSec.getInt("slot", (size / 9 - 1) * 9) : (size / 9 - 1) * 9;
        if (slot == prevSlot && currentPage > 0) {
            open(clicker, currentPage - 1);
            return;
        }

        // Check next button
        ConfigurationSection nextSec = gui.getConfigurationSection("navigation.next");
        int nextSlot = nextSec != null ? nextSec.getInt("slot", size - 1) : size - 1;
        if (slot == nextSlot && currentPage < holder.getTotalPages() - 1) {
            open(clicker, currentPage + 1);
            return;
        }

        // Check extra buttons
        ConfigurationSection buttons = gui.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String key : buttons.getKeys(false)) {
                ConfigurationSection btn = buttons.getConfigurationSection(key);
                if (btn == null) continue;
                if (btn.getInt("slot", -1) != slot) continue;
                List<String> cmds = btn.getStringList("click-commands");
                clicker.closeInventory();
                if (!cmds.isEmpty()) {
                    Bukkit.getScheduler().runTaskLater(NotKillRank.plugin, () -> {
                        for (String entry : cmds) {
                            dispatchButtonCommand(clicker, entry);
                        }
                    }, 1L);
                }
                return;
            }
        }
    }

    private static void dispatchButtonCommand(Player player, String entry) {
        String raw = entry.startsWith("/") ? entry.substring(1) : entry;
        String mode = "player";
        String cmd = raw;
        if (raw.startsWith("[") && raw.contains("] ")) {
            int end = raw.indexOf("] ");
            mode = raw.substring(1, end).toLowerCase();
            cmd = raw.substring(end + 2);
        }
        cmd = cmd.replace("{player}", player.getName());
        switch (mode) {
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            case "playerasop" -> {
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    Bukkit.dispatchCommand(player, cmd);
                } finally {
                    if (!wasOp) player.setOp(false);
                }
            }
            default -> Bukkit.dispatchCommand(player, cmd);
        }
    }
}
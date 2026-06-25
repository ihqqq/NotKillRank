package me.ihqqq.notbooster.hook.catamines;

import me.ihqqq.notbooster.NotBooster;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class CataminesHook implements Listener {

    private static final long CONFIRMATION_TTL_MILLIS = 2_500L;

    private static CataminesHook instance;

    private final Map<String, Long> confirmedBreaks = new HashMap<>();
    private boolean hooked;

    public CataminesHook() {
        instance = this;
        register();
    }

    public static CataminesHook getInstance() {
        return instance;
    }

    private void register() {
        Plugin catamines = NotBooster.plugin.getServer().getPluginManager().getPlugin("CataMines");
        if (catamines == null) catamines = NotBooster.plugin.getServer().getPluginManager().getPlugin("Catamines");
        if (catamines == null) {
            NotBooster.plugin.getLogger().warning("CataMines not found. Item gained boosters will wait for the hook.");
            return;
        }

        try {
            Class<?> eventClass = Class.forName("me.catalysmrl.catamines.api.events.CataMineBlockBreakEvent");
            EventExecutor executor = (listener, event) -> handleDynamicEvent(event);
            NotBooster.plugin.getServer().getPluginManager().registerEvent(eventClass.asSubclass(Event.class), this,
                    EventPriority.MONITOR, executor, NotBooster.plugin, true);
            hooked = true;
            NotBooster.plugin.getLogger().info("Hooked CataMines block break event for item gained boosters.");
        } catch (ClassNotFoundException e) {
            NotBooster.plugin.getLogger().warning("CataMines event API was not found. Item gained boosters are disabled until a compatible CataMines build is installed.");
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public boolean isRecentlyConfirmed(UUID playerUuid, Location location) {
        cleanup();
        return confirmedBreaks.containsKey(key(playerUuid, location));
    }

    private void handleDynamicEvent(Event event) {
        try {
            Method method = event.getClass().getMethod("getBlockBreakEvent");
            Object blockBreak = method.invoke(event);
            if (blockBreak instanceof BlockBreakEvent blockBreakEvent) {
                confirmedBreaks.put(key(blockBreakEvent.getPlayer().getUniqueId(), blockBreakEvent.getBlock().getLocation()),
                        System.currentTimeMillis() + CONFIRMATION_TTL_MILLIS);
            }
        } catch (ReflectiveOperationException e) {
            NotBooster.plugin.getLogger().warning("Could not read CataMines block break event: " + e.getMessage());
        }
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = confirmedBreaks.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) iterator.remove();
        }
    }

    private String key(UUID playerUuid, Location location) {
        return playerUuid + ":" + location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}

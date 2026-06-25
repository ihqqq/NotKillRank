package me.ihqqq.notbooster.listener;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.booster.BoosterType;
import me.ihqqq.notbooster.hook.catamines.CataminesHook;
import me.ihqqq.notbooster.manager.BoosterManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public final class ItemBoostListener implements Listener {

    public ItemBoostListener() {
        Bukkit.getPluginManager().registerEvents(this, NotBooster.plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!"BLOCK_DROP".equalsIgnoreCase(Settings.CATAMINES_REWARD_MODE)) return;
        CataminesHook hook = CataminesHook.getInstance();
        if (hook == null || !hook.isRecentlyConfirmed(event.getPlayer().getUniqueId(), event.getBlockState().getLocation())) return;

        double multiplier = BoosterManager.getInstance().resolveMultiplier(
                event.getPlayer().getUniqueId(), BoosterType.ITEM_GAINED, Settings.getMultiplierCap(BoosterType.ITEM_GAINED));
        if (multiplier <= 1.0D) return;

        for (org.bukkit.entity.Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            int boosted = scaleAmount(stack.getAmount(), multiplier, stack.getMaxStackSize());
            stack.setAmount(boosted);
            item.setItemStack(stack);
        }
    }

    private int scaleAmount(int amount, double multiplier, int maxStackSize) {
        double scaled = amount * multiplier;
        int floor = (int) Math.floor(scaled);
        double fraction = scaled - floor;
        if (ThreadLocalRandom.current().nextDouble() < fraction) floor++;
        return Math.max(1, Math.min(maxStackSize, floor));
    }
}

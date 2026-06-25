package me.ihqqq.notbooster.command.sub;

import me.ihqqq.notbooster.inventory.BoosterInventory;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class GuiBoosterSubCommand implements BoosterSubCommand {
    private final BoosterInventory inventory;

    public GuiBoosterSubCommand(BoosterInventory inventory) {
        this.inventory = inventory;
    }

    @Override public String name() { return "gui"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "invalid-sender");
            return true;
        }
        inventory.open(player);
        return true;
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}

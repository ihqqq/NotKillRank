package me.ihqqq.notbooster.command;

import me.ihqqq.notbooster.NotBooster;
import me.ihqqq.notbooster.Settings;
import me.ihqqq.notbooster.command.sub.*;
import me.ihqqq.notbooster.inventory.BoosterInventory;
import me.ihqqq.notbooster.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NotBoosterCommand implements CommandExecutor, TabCompleter {

    private final Map<String, BoosterSubCommand> subCommands = new LinkedHashMap<>();
    private final StartBoosterSubCommand startCommand = new StartBoosterSubCommand();

    public NotBoosterCommand(BoosterInventory inventory) {
        register(new GiveBoosterSubCommand());
        register(new ListBoosterSubCommand());
        register(new StopBoosterSubCommand());
        register(new ReloadBoosterSubCommand());
        if (Settings.GUI_ENABLED) register(new GuiBoosterSubCommand(inventory));
        NotBooster.plugin.getCommand("notbooster").setExecutor(this);
        NotBooster.plugin.getCommand("notbooster").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("notbooster.use")) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        BoosterSubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand != null) return subCommand.execute(sender, args);
        if (args.length == 4) return startCommand.execute(sender, args);
        sendUsage(sender);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            List<String> first = new ArrayList<>(List.of("exp", "elo", "item", "item_gained", "effect"));
            first.addAll(subCommands.keySet());
            return filter(first, args[0]);
        }
        BoosterSubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand != null) return subCommand.tabComplete(sender, args);
        return startCommand.tabComplete(sender, args);
    }

    private void register(BoosterSubCommand subCommand) {
        subCommands.put(subCommand.name(), subCommand);
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.send(sender, "command-header");
        sender.sendMessage("§7Example: §f/notbooster elo personal 2x 1h");
        sender.sendMessage("§7Admin: §f/notbooster give <player> <type> <power> <duration>");
        sender.sendMessage("§7Admin: §f/notbooster list §7| §f/notbooster stop <id> §7| §f/notbooster reload §7| §f/notbooster gui");
    }

    private List<String> filter(List<String> values, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        }
        return result;
    }
}

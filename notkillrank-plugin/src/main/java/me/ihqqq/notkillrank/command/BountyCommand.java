package me.ihqqq.notkillrank.command;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.manager.BountyManager;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.EloManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BountyCommand implements CommandExecutor, TabCompleter {

    public BountyCommand() {
        NotKillRank.getInstance().getCommand("bounty").setExecutor(this);
        NotKillRank.getInstance().getCommand("bounty").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /bounty.");
            return true;
        }
        if (!sender.hasPermission("notkillrank.use")) {
            MessageUtil.sendMessage(sender, MessageUtil.getMessage("no-permission",
                    "<red>Bạn không có quyền dùng lệnh này!"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.sendMessage(sender, "<yellow>Cách dùng: <white>/bounty <player> <elo>");
            return true;
        }
        if (args[0].equalsIgnoreCase(player.getName())) {
            MessageUtil.sendMessage(sender, "<red>Bạn không thể đặt bounty lên chính mình!");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtil.sendMessage(sender, MessageUtil.getMessage("player-not-found",
                            "<red>Không tìm thấy người chơi <yellow>{player}<red>!")
                    .replace("{player}", args[0]));
            return true;
        }

        PlayerData targetData = DataManager.getInstance().getOrCreate(target);
        if (EloManager.getInstance().isNewbie(targetData)) {
            MessageUtil.sendMessage(sender, MessageUtil.getMessage("bounty-target-protected",
                    "<red>Không thể đặt bounty lên người chơi đang được bảo vệ người mới!"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "<red>Số lượng elo không hợp lệ!");
            return true;
        }
        BountyManager.getInstance().placeBounty(player, target, amount);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                    completions.add(p.getName());
            }
        } else if (args.length == 2) {
            completions.addAll(List.of("100", "500", "1000"));
        }
        return completions;
    }
}

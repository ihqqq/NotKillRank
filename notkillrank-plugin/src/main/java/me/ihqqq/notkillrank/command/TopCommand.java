package me.ihqqq.notkillrank.command;

import me.ihqqq.notkillrank.NotKillRank;
import me.ihqqq.notkillrank.inventory.TopInventory;
import me.ihqqq.notkillrank.manager.DataManager;
import me.ihqqq.notkillrank.manager.RankManager;
import me.ihqqq.notkillrank.storage.PlayerData;
import me.ihqqq.notkillrank.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TopCommand implements CommandExecutor {

    public TopCommand() {
        NotKillRank.getInstance().getCommand("top").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            showTopText(sender);
            return true;
        }
        TopInventory.open(player);
        return true;
    }

    private void showTopText(CommandSender sender) {
        MessageUtil.sendMessage(sender, NotKillRank.getInstance().getConfig()
                .getString("messages.top-header",
                        "<dark_gray><strikethrough>          </strikethrough> <gold>Top 10 Bảng Xếp Hạng <dark_gray><strikethrough>          </strikethrough>"));

        List<PlayerData> top = DataManager.getInstance().getTopPlayers(10);
        for (int i = 0; i < top.size(); i++) {
            PlayerData data = top.get(i);
            String rank = RankManager.getInstance().getRankTag(data.getElo());
            String line = NotKillRank.getInstance().getConfig()
                    .getString("messages.top-entry",
                            "<gray>{pos}. {rank} <white>{player} <dark_gray>— <green>Elo: {elo}")
                    .replace("{pos}", String.valueOf(i + 1))
                    .replace("{rank}", rank)
                    .replace("{player}", data.getName())
                    .replace("{elo}", String.valueOf(data.getElo()));
            MessageUtil.sendMessage(sender, line);
        }
    }
}

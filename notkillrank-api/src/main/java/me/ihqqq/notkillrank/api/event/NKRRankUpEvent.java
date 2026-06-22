package me.ihqqq.notkillrank.api.event;

import me.ihqqq.notkillrank.api.IPlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NKRRankUpEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final IPlayerData data;
    private final String oldRankTag;
    private final String newRankTag;
    private final String oldRankName;
    private final String newRankName;
    private final int oldElo;
    private final int newElo;
    private boolean cancelled;

    public NKRRankUpEvent(@NotNull Player player,
                          @NotNull IPlayerData data,
                          @NotNull String oldRankTag,
                          @NotNull String newRankTag,
                          @NotNull String oldRankName,
                          @NotNull String newRankName,
                          int oldElo,
                          int newElo) {
        this.player      = player;
        this.data        = data;
        this.oldRankTag  = oldRankTag;
        this.newRankTag  = newRankTag;
        this.oldRankName = oldRankName;
        this.newRankName = newRankName;
        this.oldElo      = oldElo;
        this.newElo      = newElo;
    }

    @NotNull
    public Player getPlayer() { return player; }

    @NotNull
    public IPlayerData getData() { return data; }

    @NotNull
    public String getOldRankTag() { return oldRankTag; }

    @NotNull
    public String getNewRankTag() { return newRankTag; }

    @NotNull
    public String getOldRankName() { return oldRankName; }

    @NotNull
    public String getNewRankName() { return newRankName; }

    public int getOldElo() { return oldElo; }

    public int getNewElo() { return newElo; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    @NotNull
    public static HandlerList getHandlerList() { return HANDLERS; }
}

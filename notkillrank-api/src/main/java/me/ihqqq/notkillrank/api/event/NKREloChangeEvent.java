package me.ihqqq.notkillrank.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NKREloChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final String playerName;
    private final int oldElo;
    private int newElo;
    private final EloChangeReason reason;
    private boolean cancelled;

    public NKREloChangeEvent(@NotNull UUID playerUuid,
                             @NotNull String playerName,
                             int oldElo,
                             int newElo,
                             @NotNull EloChangeReason reason) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.oldElo     = oldElo;
        this.newElo     = newElo;
        this.reason     = reason;
    }

    @NotNull
    public UUID getPlayerUuid() { return playerUuid; }

    @NotNull
    public String getPlayerName() { return playerName; }

    public int getOldElo() { return oldElo; }

    public int getNewElo() { return newElo; }

    public void setNewElo(int newElo) { this.newElo = Math.max(0, newElo); }

    public int getDelta() { return newElo - oldElo; }

    @NotNull
    public EloChangeReason getReason() { return reason; }

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

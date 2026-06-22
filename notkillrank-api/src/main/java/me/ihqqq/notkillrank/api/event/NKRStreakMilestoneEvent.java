package me.ihqqq.notkillrank.api.event;

import me.ihqqq.notkillrank.api.IPlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NKRStreakMilestoneEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final IPlayerData killerData;
    private final int milestone;
    private boolean cancelled;

    public NKRStreakMilestoneEvent(@NotNull Player killer,
                                   @NotNull IPlayerData killerData,
                                   int milestone) {
        this.killer     = killer;
        this.killerData = killerData;
        this.milestone  = milestone;
    }

    @NotNull
    public Player getKiller() { return killer; }

    @NotNull
    public IPlayerData getKillerData() { return killerData; }

    public int getMilestone() { return milestone; }

    public int getCurrentStreak() { return killerData.getKillStreak(); }

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

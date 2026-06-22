package me.ihqqq.notkillrank.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NKRBountyPlacedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player placer;
    private final Player target;
    private int amount;
    private boolean cancelled;

    public NKRBountyPlacedEvent(@NotNull Player placer,
                                @NotNull Player target,
                                int amount) {
        this.placer = placer;
        this.target = target;
        this.amount = amount;
    }

    @NotNull
    public Player getPlacer() { return placer; }

    @NotNull
    public Player getTarget() { return target; }

    public int getAmount() { return amount; }

    public void setAmount(int amount) {
        if (amount > 0) this.amount = amount;
    }

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

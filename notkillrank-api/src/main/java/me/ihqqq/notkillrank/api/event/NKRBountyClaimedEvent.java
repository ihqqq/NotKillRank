package me.ihqqq.notkillrank.api.event;

import me.ihqqq.notkillrank.api.IPlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NKRBountyClaimedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player claimer;
    private final IPlayerData victimData;
    private final int totalAmount;

    public NKRBountyClaimedEvent(@NotNull Player claimer,
                                 @NotNull IPlayerData victimData,
                                 int totalAmount) {
        this.claimer     = claimer;
        this.victimData  = victimData;
        this.totalAmount = totalAmount;
    }

    @NotNull
    public Player getClaimer() { return claimer; }

    @NotNull
    public IPlayerData getVictimData() { return victimData; }

    public int getTotalAmount() { return totalAmount; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    @NotNull
    public static HandlerList getHandlerList() { return HANDLERS; }
}

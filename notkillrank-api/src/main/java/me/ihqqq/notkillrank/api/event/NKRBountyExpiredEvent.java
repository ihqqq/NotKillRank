package me.ihqqq.notkillrank.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NKRBountyExpiredEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID placerUuid;
    private final String targetName;
    private final int refundAmount;

    public NKRBountyExpiredEvent(@NotNull UUID placerUuid,
                                 @NotNull String targetName,
                                 int refundAmount) {
        this.placerUuid   = placerUuid;
        this.targetName   = targetName;
        this.refundAmount = refundAmount;
    }

    @NotNull
    public UUID getPlacerUuid() { return placerUuid; }

    @NotNull
    public String getTargetName() { return targetName; }

    public int getRefundAmount() { return refundAmount; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    @NotNull
    public static HandlerList getHandlerList() { return HANDLERS; }
}

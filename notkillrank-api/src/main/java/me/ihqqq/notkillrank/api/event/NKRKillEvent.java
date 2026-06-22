package me.ihqqq.notkillrank.api.event;

import me.ihqqq.notkillrank.api.IKillResult;
import me.ihqqq.notkillrank.api.IPlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NKRKillEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final Player victim;
    private final IPlayerData killerData;
    private final IPlayerData victimData;
    private final IKillResult killResult;
    private boolean cancelled;

    public NKRKillEvent(@NotNull Player killer,
                        @NotNull Player victim,
                        @NotNull IPlayerData killerData,
                        @NotNull IPlayerData victimData,
                        @Nullable IKillResult killResult) {
        this.killer     = killer;
        this.victim     = victim;
        this.killerData = killerData;
        this.victimData = victimData;
        this.killResult = killResult;
    }

    @NotNull
    public Player getKiller() { return killer; }

    @NotNull
    public Player getVictim() { return victim; }

    @NotNull
    public IPlayerData getKillerData() { return killerData; }

    @NotNull
    public IPlayerData getVictimData() { return victimData; }

    @Nullable
    public IKillResult getKillResult() { return killResult; }

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

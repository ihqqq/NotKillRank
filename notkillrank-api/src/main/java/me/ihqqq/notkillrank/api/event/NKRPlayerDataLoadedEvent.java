package me.ihqqq.notkillrank.api.event;

import me.ihqqq.notkillrank.api.IPlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NKRPlayerDataLoadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final IPlayerData data;
    private final boolean firstJoin;

    public NKRPlayerDataLoadedEvent(@NotNull Player player,
                                    @NotNull IPlayerData data,
                                    boolean firstJoin) {
        this.player    = player;
        this.data      = data;
        this.firstJoin = firstJoin;
    }

    @NotNull
    public Player getPlayer() { return player; }

    @NotNull
    public IPlayerData getData() { return data; }


    public boolean isFirstJoin() { return firstJoin; }

    @NotNull
    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    @NotNull
    public static HandlerList getHandlerList() { return HANDLERS; }
}

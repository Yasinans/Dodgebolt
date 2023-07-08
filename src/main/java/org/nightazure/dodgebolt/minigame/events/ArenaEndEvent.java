package org.nightazure.dodgebolt.minigame.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.nightazure.dodgebolt.minigame.Arena;
import org.nightazure.dodgebolt.minigame.Team;

public class ArenaEndEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    Arena arena;
    Team winningTeam;
    boolean isQuit;
    public ArenaEndEvent(Arena arena, Team winningTeam, boolean isQuit){
        this.arena = arena;
        this.winningTeam = winningTeam;
        this.isQuit = isQuit;

    }
    public Arena getArena(){
        return arena;
    }
    public Team getWinningTeam(){
        return winningTeam;
    }
    public boolean isWinByQuit(){
        return isQuit;
    }
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
}

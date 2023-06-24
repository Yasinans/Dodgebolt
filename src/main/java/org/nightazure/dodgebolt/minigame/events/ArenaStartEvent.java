package org.nightazure.dodgebolt.minigame.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.nightazure.dodgebolt.minigame.Arena;

public class ArenaStartEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    Arena arena;

    public ArenaStartEvent(Arena arena) {
        this.arena = arena;
    }
    public Arena getArena() {
        return arena;
    }
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList(){
        return handlers;
    }
}

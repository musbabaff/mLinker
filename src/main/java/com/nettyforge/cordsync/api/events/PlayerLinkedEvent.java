package com.nettyforge.cordsync.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerLinkedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String discordId;

    public PlayerLinkedEvent(Player player, String discordId) {
        super(true); 
        this.player = player;
        this.discordId = discordId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getDiscordId() {
        return discordId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}



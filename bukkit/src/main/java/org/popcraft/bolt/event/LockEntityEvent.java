package org.popcraft.bolt.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class LockEntityEvent extends Cancellable implements Event {
    private final Player player;
    private final Entity entity;
    private final boolean autoprotect;

    public LockEntityEvent(Player player, Entity entity, boolean autoprotect) {
        this.player = player;
        this.entity = entity;
        this.autoprotect = autoprotect;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Entity getEntity() {
        return this.entity;
    }

    /** Whether this event was fired from a player creating an entity with autoprotect enabled */
    public boolean isAutoprotect() {
        return this.autoprotect;
    }
}

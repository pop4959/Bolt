package org.popcraft.bolt.event;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class LockEntityEvent extends Cancellable implements Event {
    private final Player player;
    private final Entity entity;

    public LockEntityEvent(Player player, Entity entity) {
        this.player = player;
        this.entity = entity;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Entity getEntity() {
        return this.entity;
    }
}

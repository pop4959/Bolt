package org.popcraft.bolt.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class LockBlockEvent extends Cancellable implements Event {
    private final Player player;
    private final Block block;
    private final boolean autoprotect;

    public LockBlockEvent(Player player, Block block, boolean autoprotect) {
        this.player = player;
        this.block = block;
        this.autoprotect = autoprotect;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Block getBlock() {
        return this.block;
    }

    /** Whether this event was fired from a player placing a block with autoprotect enabled */
    public boolean isAutoprotect() {
        return this.autoprotect;
    }
}

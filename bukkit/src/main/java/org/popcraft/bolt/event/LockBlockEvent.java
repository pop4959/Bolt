package org.popcraft.bolt.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class LockBlockEvent extends Cancellable implements Event {
    private final Player player;
    private final Block block;

    public LockBlockEvent(Player player, Block block) {
        this.player = player;
        this.block = block;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Block getBlock() {
        return this.block;
    }
}

package org.popcraft.bolt.util;

import org.bukkit.block.Block;
import org.popcraft.bolt.protection.BlockProtection;

import java.util.HashMap;
import java.util.UUID;

public record ProtectableBlock(Block block) implements Protectable {
    @Override
    public String getTypeName() {
        return block.getType().name();
    }

    @Override
    public BlockProtection createProtection(final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new BlockProtection(UUID.randomUUID(), owner, type, now, now, new HashMap<>(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getType().name());
    }
}

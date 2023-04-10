package org.popcraft.bolt;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.popcraft.bolt.protection.Protection;

public interface BoltAPI {
    boolean isProtectable(final Block block);

    boolean isProtectable(final Entity entity);

    Protection findProtection(final Block block);

    Protection findProtection(final Entity entity);
}

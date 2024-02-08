package org.popcraft.bolt.util;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.popcraft.bolt.protection.Protection;

import java.util.UUID;

public sealed interface Protectable permits ProtectableBlock, ProtectableEntity {
    // Equivalent to getType().name() for both Block and Entity
    String getTypeName();

    Protection createProtection(final UUID owner, final String type);
}

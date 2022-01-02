package org.popcraft.bolt.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.popcraft.bolt.data.defaults.DefaultProtectionType;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.util.BlockLocation;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public final class BukkitAdapter {
    private BukkitAdapter() {
    }

    public static BlockProtection createPrivateBlockProtection(final Block block, final Entity owner) {
        return createBlockProtection(block, owner, DefaultProtectionType.PRIVATE.type());
    }

    public static BlockProtection createBlockProtection(final Block block, final Entity owner, final String type) {
        final BlockLocation location = blockLocation(block);
        return new BlockProtection(UUID.randomUUID(), owner.getUniqueId().toString(), type, new HashMap<>(), block.getType().toString(), location.world(), location.x(), location.y(), location.z());
    }

    public static BlockLocation blockLocation(final Block block) {
        return new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockLocation blockLocation(final Location location) {
        Objects.requireNonNull(location.getWorld());
        return new BlockLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}

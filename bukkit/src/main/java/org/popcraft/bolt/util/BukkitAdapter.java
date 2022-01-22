package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.util.defaults.DefaultProtectionType;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class BukkitAdapter {
    private BukkitAdapter() {
    }

    public static BlockProtection createPrivateBlockProtection(final Block block, final Entity owner) {
        return createBlockProtection(block, owner, DefaultProtectionType.PRIVATE.type());
    }

    public static BlockProtection createBlockProtection(final Block block, final Entity owner, final String type) {
        final BlockLocation location = blockLocation(block);
        return new BlockProtection(UUID.randomUUID(), owner.getUniqueId(), type, new HashMap<>(), block.getType().toString(), location.world(), location.x(), location.y(), location.z());
    }

    public static BlockLocation blockLocation(final Block block) {
        return new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockLocation blockLocation(final BlockState blockState) {
        return new BlockLocation(blockState.getWorld().getName(), blockState.getX(), blockState.getY(), blockState.getZ());
    }

    public static BlockLocation blockLocation(final Location location) {
        Objects.requireNonNull(location.getWorld());
        return new BlockLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @SuppressWarnings("deprecation")
    public static UUID playerUUID(final String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    public static Optional<String> playerName(final UUID uuid) {
        return Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName());
    }
}

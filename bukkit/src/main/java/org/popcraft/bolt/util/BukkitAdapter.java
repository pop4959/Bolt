package org.popcraft.bolt.util;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.ProfileCache;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public final class BukkitAdapter {
    private BukkitAdapter() {
    }

    public static BlockProtection createBlockProtection(final Block block, final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new BlockProtection(UUID.randomUUID(), owner, type, now, now, new HashMap<>(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getType().name());
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

    public static BlockLocation blockLocation(final BlockProtection blockProtection) {
        return new BlockLocation(blockProtection.getWorld(), blockProtection.getX(), blockProtection.getY(), blockProtection.getZ());
    }

    public static EntityProtection createEntityProtection(final Entity entity, final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new EntityProtection(entity.getUniqueId(), owner, type, now, now, new HashMap<>(), entity.getType().name());
    }

    public static UUID findPlayerUniqueId(final String player) {
        if (player == null) {
            return null;
        }
        try {
            return UUID.fromString(player);
        } catch (final IllegalArgumentException e) {
            final ProfileCache profileCache = JavaPlugin.getPlugin(BoltPlugin.class).getProfileCache();
            final UUID cached = profileCache.getUniqueId(player);
            if (cached != null) {
                return cached;
            }
            final OfflinePlayer offlinePlayer = PaperUtil.getOfflinePlayer(player);
            if (offlinePlayer != null) {
                profileCache.add(offlinePlayer.getUniqueId(), offlinePlayer.getName());
            }
            return offlinePlayer == null ? null : offlinePlayer.getUniqueId();
        }
    }
}

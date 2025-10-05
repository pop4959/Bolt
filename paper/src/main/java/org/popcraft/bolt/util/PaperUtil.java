package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.CompletableFuture;

public class PaperUtil {
    private static final boolean CONFIG_EXISTS = classExists("com.destroystokyo.paper.PaperConfig") || classExists("io.papermc.paper.configuration.Configuration");
    private static boolean getOfflinePlayerIfCachedExists;
    private static boolean getChunkAtAsyncExists;
    private static boolean teleportAsyncExists;

    static {
        try {
            Bukkit.class.getMethod("getOfflinePlayerIfCached", String.class);
            getOfflinePlayerIfCachedExists = true;
        } catch (NoSuchMethodException e) {
            getOfflinePlayerIfCachedExists = false;
        }
        try {
            World.class.getMethod("getChunkAtAsync", int.class, int.class);
            getChunkAtAsyncExists = true;
        } catch (NoSuchMethodException e) {
            getChunkAtAsyncExists = false;
        }
        try {
            Entity.class.getMethod("teleportAsync", Location.class);
            teleportAsyncExists = true;
        } catch (NoSuchMethodException e) {
            teleportAsyncExists = false;
        }
    }

    private PaperUtil() {
    }

    public static boolean isPaper() {
        return CONFIG_EXISTS;
    }

    public static OfflinePlayer getOfflinePlayer(final String name) {
        if (getOfflinePlayerIfCachedExists) {
            return Bukkit.getOfflinePlayerIfCached(name);
        } else {
            return Bukkit.getOfflinePlayer(name);
        }
    }

    public static CompletableFuture<Chunk> getChunkAtAsync(final World world, final int x, final int z) {
        if (getChunkAtAsyncExists) {
            return world.getChunkAtAsync(x, z);
        } else {
            return CompletableFuture.completedFuture(world.getChunkAt(x, z));
        }
    }

    public static CompletableFuture<Boolean> teleportAsync(final Entity entity, final Location location) {
        if (teleportAsyncExists) {
            return entity.teleportAsync(location);
        } else {
            return CompletableFuture.completedFuture(entity.teleport(location));
        }
    }

    private static boolean classExists(final String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

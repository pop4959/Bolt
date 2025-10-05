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
    private static boolean teleportAsyncExists;

    static {
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

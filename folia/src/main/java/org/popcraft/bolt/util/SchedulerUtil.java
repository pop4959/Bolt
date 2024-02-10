package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Executor;

public class SchedulerUtil {
    private SchedulerUtil() {
    }

    public static void schedule(final Plugin plugin, final CommandSender sender, final Runnable runnable) {
        schedule(plugin, sender, runnable, 0);
    }

    public static void schedule(final Plugin plugin, final CommandSender sender, final Runnable runnable, final long delay) {
        if (FoliaUtil.isFolia()) {
            if (sender instanceof final Player player) {
                player.getScheduler().execute(plugin, runnable, () -> {
                }, delay);
            } else {
                if (delay <= 0) {
                    Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
                } else {
                    Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), delay);
                }
            }
        } else {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay);
        }
    }

    public static void schedule(final Plugin plugin, final Location location, final Runnable runnable) {
        if (FoliaUtil.isFolia()) {
            Bukkit.getRegionScheduler().execute(plugin, location, runnable);
        } else {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable);
        }
    }

    public static void schedule(final Plugin plugin, final Runnable runnable, final long delay, final long interval) {
        if (FoliaUtil.isFolia()) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, interval);
        } else {
            Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, interval);
        }
    }

    public static Executor executor(final Plugin plugin, final CommandSender sender) {
        return command -> schedule(plugin, sender, command);
    }
}

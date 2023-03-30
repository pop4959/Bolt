package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Executor;

public class SchedulerUtil {
    private SchedulerUtil() {
    }

    public static void schedule(final Plugin plugin, final CommandSender sender, final Runnable runnable) {
        if (FoliaUtil.isFolia()) {
            if (sender instanceof final Player player) {
                player.getScheduler().execute(plugin, runnable, () -> {
                }, 0);
            } else {
                Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
            }
        } else {
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, runnable);
        }
    }

    public static Executor executor(final Plugin plugin, final CommandSender sender) {
        return command -> schedule(plugin, sender, command);
    }
}

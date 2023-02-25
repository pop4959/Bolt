package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.BoltPlugin;

import java.util.concurrent.Executor;

public class BukkitMainThreadExecutor implements Executor {
    private static final BukkitMainThreadExecutor EXECUTOR = new BukkitMainThreadExecutor();

    public static Executor get() {
        return EXECUTOR;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(JavaPlugin.getPlugin(BoltPlugin.class), command);
    }
}

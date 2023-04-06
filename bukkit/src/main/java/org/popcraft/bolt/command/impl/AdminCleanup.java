package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.ChunkPos;
import org.popcraft.bolt.util.PaperUtil;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class AdminCleanup extends BoltCommand {
    private static final int PERMITS = 10;
    private static final Semaphore WORKING = new Semaphore(PERMITS);

    public AdminCleanup(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final Store store = plugin.getBolt().getStore();
        final Collection<BlockProtection> protections = store.loadBlockProtections().join();
        final long count = protections.size();
        BoltComponents.sendMessage(
                sender,
                Translation.CLEANUP_START,
                Placeholder.component(Translation.Placeholder.COUNT, Component.text(count))
        );
        final Map<String, World> worlds = new HashMap<>();
        final Map<ChunkPos, List<BlockProtection>> blockProtectionsByChunk = new HashMap<>();
        final long start = System.currentTimeMillis();
        for (final BlockProtection blockProtection : protections) {
            final World world = plugin.getServer().getWorld(blockProtection.getWorld());
            if (world == null) {
                continue;
            }
            final String worldName = world.getName();
            worlds.put(worldName, world);
            final int x = blockProtection.getX() >> 4;
            final int z = blockProtection.getZ() >> 4;
            final ChunkPos chunkPos = new ChunkPos(worldName, x, z);
            blockProtectionsByChunk.computeIfAbsent(chunkPos, ignored -> new ArrayList<>());
            blockProtectionsByChunk.get(chunkPos).add(blockProtection);
        }
        final AtomicLong removed = new AtomicLong();
        CompletableFuture.runAsync(() -> {
            for (final Map.Entry<ChunkPos, List<BlockProtection>> entry : blockProtectionsByChunk.entrySet()) {
                final ChunkPos chunkPos = entry.getKey();
                final List<BlockProtection> blockProtections = entry.getValue();
                try {
                    WORKING.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                final World world = worlds.get(chunkPos.world());
                final int x = chunkPos.x();
                final int z = chunkPos.z();
                PaperUtil.getChunkAtAsync(world, x, z)
                        .thenAccept(ignored -> {
                            for (BlockProtection blockProtection : blockProtections) {
                                final Block block = world.getBlockAt(blockProtection.getX(), blockProtection.getY(), blockProtection.getZ());
                                if (!blockProtection.getBlock().equals(block.getType().name())) {
                                    store.removeBlockProtection(blockProtection);
                                    removed.incrementAndGet();
                                    SchedulerUtil.schedule(plugin, sender, () -> BoltComponents.sendMessage(
                                            sender,
                                            Translation.CLEANUP_REMOVE,
                                            Placeholder.component(Translation.Placeholder.RAW_PROTECTION, Protections.raw(blockProtection))
                                    ));
                                }
                            }
                        })
                        .thenRun(WORKING::release);
            }
            WORKING.acquireUninterruptibly(PERMITS);
        }).thenRunAsync(() -> {
            final long finish = System.currentTimeMillis();
            final long seconds = (finish - start) / 1000;
            BoltComponents.sendMessage(
                    sender,
                    Translation.CLEANUP_COMPLETE,
                    Placeholder.component(Translation.Placeholder.COUNT, Component.text(removed.get())),
                    Placeholder.component(Translation.Placeholder.SECONDS, Component.text(seconds))
            );
        }, SchedulerUtil.executor(plugin, sender));
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_CLEANUP,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin cleanup"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_CLEANUP);
    }
}

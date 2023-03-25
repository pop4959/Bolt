package org.popcraft.bolt.command.impl;

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
import org.popcraft.bolt.util.BukkitMainThreadExecutor;
import org.popcraft.bolt.util.ChunkPos;
import org.popcraft.bolt.util.PaperUtil;

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
        BoltComponents.sendMessage(sender, Translation.CLEANUP_START, Placeholder.unparsed(Translation.Placeholder.COUNT, String.valueOf(count)));
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
                                    BukkitMainThreadExecutor.get().execute(() -> BoltComponents.sendMessage(sender, Translation.CLEANUP_REMOVE, Placeholder.unparsed(Translation.Placeholder.RAW_PROTECTION, blockProtection.toString())));
                                }
                            }
                        })
                        .thenRun(WORKING::release);
            }
            WORKING.acquireUninterruptibly(PERMITS);
        }).thenRunAsync(() -> {
            final long finish = System.currentTimeMillis();
            final long seconds = (finish - start) / 1000;
            BoltComponents.sendMessage(sender, Translation.CLEANUP_COMPLETE, Placeholder.unparsed(Translation.Placeholder.COUNT, String.valueOf(removed.get())), Placeholder.unparsed(Translation.Placeholder.SECONDS, String.valueOf(seconds)));
        }, BukkitMainThreadExecutor.get());
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        return Collections.emptyList();
    }
}

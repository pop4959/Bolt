package org.popcraft.bolt.data;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Metrics;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

public class SimpleProtectionCache implements Store {
    private final Map<BlockLocation, BlockProtection> cachedBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, EntityProtection> cachedEntities = new ConcurrentHashMap<>();
    private final Executor executorThread = Executors.newSingleThreadExecutor();
    private final Store backingStore;

    public SimpleProtectionCache(final Store backingStore) {
        this.backingStore = backingStore;
        CompletableFuture.runAsync(() -> {
            backingStore.loadBlockProtections().forEach(blockProtection -> cachedBlocks.putIfAbsent(BukkitAdapter.blockLocation(blockProtection), blockProtection));
            backingStore.loadEntityProtections().forEach(entityProtection -> cachedEntities.putIfAbsent(entityProtection.getId(), entityProtection));
        }, executorThread);
    }

    @Override
    public BlockProtection loadBlockProtection(BlockLocation location) {
        final long startTimeNanos = System.nanoTime();

        final BlockProtection blockProtection = cachedBlocks.get(location);

        final boolean hit = blockProtection != null;
        if (hit) {
            final long timeNanos = System.nanoTime() - startTimeNanos;
            final double timeMillis = timeNanos / 1e6d;
            LogManager.getLogManager().getLogger("").info(() -> "Loading block protection took %.3f ms".formatted(timeMillis));
        }
        Metrics.recordProtectionAccess(hit);

        return blockProtection;
    }

    @Override
    public Collection<BlockProtection> loadBlockProtections() {
        return cachedBlocks.values();
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        final long startTimeNanos = System.nanoTime();

        cachedBlocks.put(BukkitAdapter.blockLocation(protection), protection);
        CompletableFuture.runAsync(() -> backingStore.saveBlockProtection(protection), executorThread);

        final long timeNanos = System.nanoTime() - startTimeNanos;
        final double timeMillis = timeNanos / 1e6d;
        LogManager.getLogManager().getLogger("").info(() -> "Saving block protection took %.3f ms".formatted(timeMillis));
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        final long startTimeNanos = System.nanoTime();

        cachedBlocks.remove(BukkitAdapter.blockLocation(protection));
        CompletableFuture.runAsync(() -> backingStore.removeBlockProtection(protection), executorThread);

        final long timeNanos = System.nanoTime() - startTimeNanos;
        final double timeMillis = timeNanos / 1e6d;
        LogManager.getLogManager().getLogger("").info(() -> "Removing block protection took %.3f ms".formatted(timeMillis));
    }

    @Override
    public EntityProtection loadEntityProtection(UUID id) {
        final long startTimeNanos = System.nanoTime();

        final EntityProtection entityProtection = cachedEntities.get(id);

        final boolean hit = entityProtection != null;
        if (hit) {
            final long timeNanos = System.nanoTime() - startTimeNanos;
            final double timeMillis = timeNanos / 1e6d;
            LogManager.getLogManager().getLogger("").info(() -> "Loading entity protection took %.3f ms".formatted(timeMillis));
        }
        Metrics.recordProtectionAccess(hit);

        return entityProtection;
    }

    @Override
    public Collection<EntityProtection> loadEntityProtections() {
        return cachedEntities.values();
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        final long startTimeNanos = System.nanoTime();

        cachedEntities.put(protection.getId(), protection);
        CompletableFuture.runAsync(() -> backingStore.saveEntityProtection(protection), executorThread);

        final long timeNanos = System.nanoTime() - startTimeNanos;
        final double timeMillis = timeNanos / 1e6d;
        LogManager.getLogManager().getLogger("").info(() -> "Saving entity protection took %.3f ms".formatted(timeMillis));
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        final long startTimeNanos = System.nanoTime();

        cachedEntities.remove(protection.getId());
        CompletableFuture.runAsync(() -> backingStore.removeEntityProtection(protection), executorThread);

        final long timeNanos = System.nanoTime() - startTimeNanos;
        final double timeMillis = timeNanos / 1e6d;
        LogManager.getLogManager().getLogger("").info(() -> "Removing entity protection took %.3f ms".formatted(timeMillis));
    }
}

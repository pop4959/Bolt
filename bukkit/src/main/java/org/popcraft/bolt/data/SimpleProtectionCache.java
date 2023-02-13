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
        final BlockProtection blockProtection = cachedBlocks.get(location);
        Metrics.recordProtectionAccess(blockProtection != null);
        return blockProtection;
    }

    @Override
    public Collection<BlockProtection> loadBlockProtections() {
        return cachedBlocks.values();
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        cachedBlocks.put(BukkitAdapter.blockLocation(protection), protection);
        CompletableFuture.runAsync(() -> backingStore.saveBlockProtection(protection), executorThread);
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        cachedBlocks.remove(BukkitAdapter.blockLocation(protection));
        CompletableFuture.runAsync(() -> backingStore.removeBlockProtection(protection), executorThread);
    }

    @Override
    public EntityProtection loadEntityProtection(UUID id) {
        final EntityProtection entityProtection = cachedEntities.get(id);
        Metrics.recordProtectionAccess(entityProtection != null);
        return entityProtection;
    }

    @Override
    public Collection<EntityProtection> loadEntityProtections() {
        return cachedEntities.values();
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        cachedEntities.put(protection.getId(), protection);
        CompletableFuture.runAsync(() -> backingStore.saveEntityProtection(protection), executorThread);
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        cachedEntities.remove(protection.getId());
        CompletableFuture.runAsync(() -> backingStore.removeEntityProtection(protection), executorThread);
    }
}

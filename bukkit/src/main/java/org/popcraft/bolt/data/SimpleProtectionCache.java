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

public class SimpleProtectionCache implements Store {
    private final Map<BlockLocation, BlockProtection> cachedBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, EntityProtection> cachedEntities = new ConcurrentHashMap<>();
    private final Store backingStore;

    public SimpleProtectionCache(final Store backingStore) {
        this.backingStore = backingStore;
        backingStore.loadBlockProtections().join().forEach(blockProtection -> cachedBlocks.putIfAbsent(BukkitAdapter.blockLocation(blockProtection), blockProtection));
        backingStore.loadEntityProtections().join().forEach(entityProtection -> cachedEntities.putIfAbsent(entityProtection.getId(), entityProtection));
    }

    @Override
    public CompletableFuture<BlockProtection> loadBlockProtection(BlockLocation location) {
        final BlockProtection blockProtection = cachedBlocks.get(location);
        Metrics.recordProtectionAccess(blockProtection != null);
        return CompletableFuture.completedFuture(blockProtection);
    }

    @Override
    public CompletableFuture<Collection<BlockProtection>> loadBlockProtections() {
        return CompletableFuture.completedFuture(cachedBlocks.values());
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        cachedBlocks.put(BukkitAdapter.blockLocation(protection), protection);
        backingStore.saveBlockProtection(protection);
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        cachedBlocks.remove(BukkitAdapter.blockLocation(protection));
        backingStore.removeBlockProtection(protection);
    }

    @Override
    public CompletableFuture<EntityProtection> loadEntityProtection(UUID id) {
        final EntityProtection entityProtection = cachedEntities.get(id);
        Metrics.recordProtectionAccess(entityProtection != null);
        return CompletableFuture.completedFuture(entityProtection);
    }

    @Override
    public CompletableFuture<Collection<EntityProtection>> loadEntityProtections() {
        return CompletableFuture.completedFuture(cachedEntities.values());
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        cachedEntities.put(protection.getId(), protection);
        backingStore.saveEntityProtection(protection);
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        cachedEntities.remove(protection.getId());
        backingStore.removeEntityProtection(protection);
    }

    @Override
    public CompletableFuture<Void> flush() {
        return backingStore.flush();
    }
}

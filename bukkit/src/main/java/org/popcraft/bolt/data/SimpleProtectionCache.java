package org.popcraft.bolt.data;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.access.AccessList;
import org.popcraft.bolt.util.Group;
import org.popcraft.bolt.util.Metrics;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleProtectionCache implements Store {
    private final Map<BlockLocation, BlockProtection> cachedBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, EntityProtection> cachedEntities = new ConcurrentHashMap<>();
    private final Map<String, Group> cachedGroups = new ConcurrentHashMap<>();
    private final Map<UUID, AccessList> cachedAccessLists = new ConcurrentHashMap<>();
    private final Store backingStore;

    public SimpleProtectionCache(final Store backingStore) {
        this.backingStore = backingStore;
        backingStore.loadBlockProtections().join().forEach(blockProtection -> cachedBlocks.putIfAbsent(BukkitAdapter.blockLocation(blockProtection), blockProtection));
        backingStore.loadEntityProtections().join().forEach(entityProtection -> cachedEntities.putIfAbsent(entityProtection.getId(), entityProtection));
        backingStore.loadGroups().join().forEach(group -> cachedGroups.putIfAbsent(group.getName(), group));
        backingStore.loadAccessLists().join().forEach(accessList -> cachedAccessLists.putIfAbsent(accessList.getOwner(), accessList));
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
    public CompletableFuture<Group> loadGroup(String group) {
        return CompletableFuture.completedFuture(cachedGroups.get(group));
    }

    @Override
    public CompletableFuture<Collection<Group>> loadGroups() {
        return CompletableFuture.completedFuture(cachedGroups.values());
    }

    @Override
    public void saveGroup(Group group) {
        cachedGroups.put(group.getName(), group);
        backingStore.saveGroup(group);
    }

    @Override
    public void removeGroup(Group group) {
        cachedGroups.remove(group.getName());
        backingStore.removeGroup(group);
    }

    @Override
    public CompletableFuture<AccessList> loadAccessList(UUID owner) {
        return CompletableFuture.completedFuture(cachedAccessLists.get(owner));
    }

    @Override
    public CompletableFuture<Collection<AccessList>> loadAccessLists() {
        return CompletableFuture.completedFuture(cachedAccessLists.values());
    }

    @Override
    public void saveAccessList(AccessList accessList) {
        cachedAccessLists.put(accessList.getOwner(), accessList);
        backingStore.saveAccessList(accessList);
    }

    @Override
    public void removeAccessList(AccessList accessList) {
        cachedAccessLists.remove(accessList.getOwner());
        backingStore.removeAccessList(accessList);
    }

    @Override
    public long pendingSave() {
        return backingStore.pendingSave();
    }

    @Override
    public CompletableFuture<Void> flush() {
        return backingStore.flush();
    }
}

package org.popcraft.bolt.data;

import org.popcraft.bolt.access.AccessList;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.Group;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStore implements Store {
    private final Map<BlockLocation, BlockProtection> blockProtectionMap = new ConcurrentHashMap<>();
    private final Map<UUID, EntityProtection> entityProtectionMap = new ConcurrentHashMap<>();
    private final Map<String, Group> groupMap = new ConcurrentHashMap<>();
    private final Map<UUID, AccessList> accessListMap = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<BlockProtection> loadBlockProtection(final BlockLocation location) {
        return CompletableFuture.completedFuture(blockProtectionMap.get(location));
    }

    @Override
    public CompletableFuture<Collection<BlockProtection>> loadBlockProtections() {
        return CompletableFuture.completedFuture(List.copyOf(blockProtectionMap.values()));
    }

    @Override
    public void saveBlockProtection(final BlockProtection protection) {
        final BlockLocation blockLocation = new BlockLocation(protection.getWorld(), protection.getX(), protection.getY(), protection.getZ());
        blockProtectionMap.put(blockLocation, protection);
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        final BlockLocation blockLocation = new BlockLocation(protection.getWorld(), protection.getX(), protection.getY(), protection.getZ());
        blockProtectionMap.remove(blockLocation);
    }

    @Override
    public CompletableFuture<EntityProtection> loadEntityProtection(UUID id) {
        return CompletableFuture.completedFuture(entityProtectionMap.get(id));
    }

    @Override
    public CompletableFuture<Collection<EntityProtection>> loadEntityProtections() {
        return CompletableFuture.completedFuture(List.copyOf(entityProtectionMap.values()));
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        entityProtectionMap.put(protection.getId(), protection);
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        entityProtectionMap.remove(protection.getId());
    }

    @Override
    public CompletableFuture<Group> loadGroup(String group) {
        return CompletableFuture.completedFuture(groupMap.get(group));
    }

    @Override
    public CompletableFuture<Collection<Group>> loadGroups() {
        return CompletableFuture.completedFuture(groupMap.values());
    }

    @Override
    public void saveGroup(Group group) {
        groupMap.put(group.getName(), group);
    }

    @Override
    public void removeGroup(Group group) {
        groupMap.remove(group.getName());
    }

    @Override
    public CompletableFuture<AccessList> loadAccessList(UUID owner) {
        return CompletableFuture.completedFuture(accessListMap.get(owner));
    }

    @Override
    public CompletableFuture<Collection<AccessList>> loadAccessLists() {
        return CompletableFuture.completedFuture(accessListMap.values());
    }

    @Override
    public void saveAccessList(AccessList accessList) {
        accessListMap.put(accessList.getOwner(), accessList);
    }

    @Override
    public void removeAccessList(AccessList accessList) {
        accessListMap.remove(accessList.getOwner());
    }

    @Override
    public long pendingSave() {
        return 0;
    }

    @Override
    public CompletableFuture<Void> flush() {
        return CompletableFuture.completedFuture(null);
    }
}

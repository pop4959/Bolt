package org.popcraft.bolt.data;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.AccessList;
import org.popcraft.bolt.util.Group;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Store {
    CompletableFuture<BlockProtection> loadBlockProtection(BlockLocation location);

    CompletableFuture<Collection<BlockProtection>> loadBlockProtections();

    void saveBlockProtection(BlockProtection protection);

    void removeBlockProtection(BlockProtection protection);

    CompletableFuture<EntityProtection> loadEntityProtection(UUID id);

    CompletableFuture<Collection<EntityProtection>> loadEntityProtections();

    void saveEntityProtection(EntityProtection protection);

    void removeEntityProtection(EntityProtection protection);

    CompletableFuture<Group> loadGroup(String group);

    CompletableFuture<Collection<Group>> loadGroups();

    void saveGroup(Group group);

    void removeGroup(Group group);

    CompletableFuture<AccessList> loadAccessList(UUID owner);

    CompletableFuture<Collection<AccessList>> loadAccessLists();

    void saveAccessList(AccessList accessList);

    void removeAccessList(AccessList accessList);

    long pendingSave();

    CompletableFuture<Void> flush();
}

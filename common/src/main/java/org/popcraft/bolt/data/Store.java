package org.popcraft.bolt.data;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;

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

    long pendingSave();

    CompletableFuture<Void> flush();
}

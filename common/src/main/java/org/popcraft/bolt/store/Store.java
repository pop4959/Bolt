package org.popcraft.bolt.store;

import org.popcraft.bolt.util.Access;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Store {
    Optional<Access> loadAccess(String type);

    List<Access> loadAccess();

    void saveAccess(Access access);

    Optional<BlockProtection> loadBlockProtection(UUID id);

    Optional<BlockProtection> loadBlockProtection(BlockLocation location);

    List<BlockProtection> loadBlockProtections();

    void saveBlockProtection(BlockProtection protection);

    void removeBlockProtection(BlockProtection protection);

    Optional<EntityProtection> loadEntityProtection(UUID id);

    List<EntityProtection> loadEntityProtections();

    void saveEntityProtection(EntityProtection protection);
}
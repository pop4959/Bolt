package org.popcraft.bolt.data;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface Store {
    Optional<BlockProtection> loadBlockProtection(BlockLocation location);

    Collection<BlockProtection> loadBlockProtections();

    void saveBlockProtection(BlockProtection protection);

    void removeBlockProtection(BlockProtection protection);

    Optional<EntityProtection> loadEntityProtection(UUID id);

    Collection<EntityProtection> loadEntityProtections();

    void saveEntityProtection(EntityProtection protection);

    void removeEntityProtection(EntityProtection protection);
}

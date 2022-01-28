package org.popcraft.bolt.data;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStore implements Store {
    private final Map<BlockLocation, BlockProtection> blockProtectionMap = new ConcurrentHashMap<>();
    private final Map<UUID, EntityProtection> entityProtectionMap = new ConcurrentHashMap<>();

    @Override
    public Optional<BlockProtection> loadBlockProtection(final BlockLocation location) {
        return Optional.ofNullable(blockProtectionMap.get(location));
    }

    @Override
    public List<BlockProtection> loadBlockProtections() {
        return List.copyOf(blockProtectionMap.values());
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
    public Optional<EntityProtection> loadEntityProtection(UUID id) {
        return Optional.ofNullable(entityProtectionMap.get(id));
    }

    @Override
    public List<EntityProtection> loadEntityProtections() {
        return List.copyOf(entityProtectionMap.values());
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        entityProtectionMap.put(protection.getId(), protection);
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        entityProtectionMap.remove(protection.getId());
    }
}

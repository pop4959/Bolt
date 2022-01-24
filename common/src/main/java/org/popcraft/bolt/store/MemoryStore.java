package org.popcraft.bolt.store;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MemoryStore implements Store {
    private final Map<UUID, BlockProtection> blockProtectionMap = new HashMap<>();
    private final Map<BlockLocation, BlockProtection> blockLocationProtectionMap = new HashMap<>();
    private final Map<UUID, EntityProtection> entityProtectionMap = new HashMap<>();

    @Override
    public Optional<BlockProtection> loadBlockProtection(final UUID id) {
        return Optional.ofNullable(blockProtectionMap.get(id));
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(final BlockLocation location) {
        return Optional.ofNullable(blockLocationProtectionMap.get(location));
    }

    @Override
    public List<BlockProtection> loadBlockProtections() {
        return List.copyOf(blockProtectionMap.values());
    }

    @Override
    public void saveBlockProtection(final BlockProtection protection) {
        blockProtectionMap.put(protection.getId(), protection);
        final BlockLocation blockLocation = new BlockLocation(protection.getWorld(), protection.getX(), protection.getY(), protection.getZ());
        blockLocationProtectionMap.put(blockLocation, protection);
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        blockProtectionMap.remove(protection.getId());
        final BlockLocation blockLocation = new BlockLocation(protection.getWorld(), protection.getX(), protection.getY(), protection.getZ());
        blockLocationProtectionMap.remove(blockLocation);
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
}

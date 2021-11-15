package org.popcraft.bolt.data.store;

import org.popcraft.bolt.data.Access;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.util.BlockLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MemoryStore implements Store {
    private final Map<String, Access> accessMap = new HashMap<>();
    private final Map<UUID, BlockProtection> uuidProtectionMap = new HashMap<>();
    private final Map<BlockLocation, BlockProtection> locationProtectionMap = new HashMap<>();

    @Override
    public Optional<Access> loadAccess(final String type) {
        return Optional.ofNullable(accessMap.get(type));
    }

    @Override
    public List<Access> loadAccess() {
        return List.copyOf(accessMap.values());
    }

    @Override
    public void saveAccess(Access access) {
        accessMap.put(access.type(), access);
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(final UUID id) {
        return Optional.ofNullable(uuidProtectionMap.get(id));
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(final BlockLocation location) {
        return Optional.ofNullable(locationProtectionMap.get(location));
    }

    @Override
    public List<BlockProtection> loadBlockProtections() {
        return List.copyOf(uuidProtectionMap.values());
    }

    @Override
    public void saveBlockProtection(final BlockProtection protection) {
        uuidProtectionMap.put(protection.getId(), protection);
        final BlockLocation blockLocation = new BlockLocation(protection.getWorld(), protection.getX(), protection.getY(), protection.getZ());
        locationProtectionMap.put(blockLocation, protection);
    }
}

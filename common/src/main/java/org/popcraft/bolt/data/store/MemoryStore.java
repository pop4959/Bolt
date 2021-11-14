package org.popcraft.bolt.data.store;

import org.popcraft.bolt.data.Access;
import org.popcraft.bolt.data.protection.Protection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MemoryStore implements Store {
    private final Map<String, Access> accessMap = new HashMap<>();
    private final Map<UUID, Protection> protectionMap = new HashMap<>();

    @Override
    public List<Access> loadAccess() {
        return List.copyOf(accessMap.values());
    }

    @Override
    public void saveAccess(Access access) {
        accessMap.put(access.type(), access);
    }

    @Override
    public Optional<Protection> loadProtection(UUID id) {
        return Optional.ofNullable(protectionMap.get(id));
    }

    @Override
    public List<Protection> loadProtections() {
        return List.copyOf(protectionMap.values());
    }

    @Override
    public void saveProtection(Protection protection) {
        protectionMap.put(protection.getId(), protection);
    }
}

package org.popcraft.bolt.registry;

import org.popcraft.bolt.data.Access;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AccessRegistry {
    private final Map<String, Access> accessMap = new HashMap<>();

    public void register(final String type, final Access access) {
        accessMap.put(type, access);
    }

    public void unregister(final String type) {
        accessMap.remove(type);
    }

    public Optional<Access> getAccess(String type) {
        return Optional.ofNullable(accessMap.get(type));
    }
}

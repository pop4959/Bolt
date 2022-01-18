package org.popcraft.bolt;

import org.popcraft.bolt.util.Access;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AccessRegistry {
    private final Map<String, Access> accessMap = new HashMap<>();

    public void register(final String type, final Set<String> permissions) {
        accessMap.put(type, new Access(type, permissions));
    }

    public void unregister(final String type) {
        accessMap.remove(type);
    }

    public Optional<Access> get(String type) {
        return Optional.ofNullable(accessMap.get(type));
    }
}

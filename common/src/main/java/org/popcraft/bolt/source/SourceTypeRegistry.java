package org.popcraft.bolt.source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SourceTypeRegistry {
    private final Map<String, SourceType> sourceTypes = new HashMap<>();

    public void registerSourceType(final String sourceType, final boolean restricted) {
        sourceTypes.put(sourceType, new SourceType(sourceType, restricted));
    }

    public void unregisterAll() {
        sourceTypes.clear();
    }

    public Optional<SourceType> getSourceByName(String name) {
        return Optional.ofNullable(sourceTypes.get(name));
    }

    public Set<String> names() {
        return new HashSet<>(sourceTypes.keySet());
    }

    public Set<SourceType> sourceTypes() {
        return new HashSet<>(sourceTypes.values());
    }
}

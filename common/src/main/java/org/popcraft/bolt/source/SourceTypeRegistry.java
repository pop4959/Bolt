package org.popcraft.bolt.source;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SourceTypeRegistry {
    private final Set<String> sourceTypes = new HashSet<>(List.of(
            SourceType.PLAYER,
            SourceType.PASSWORD,
            SourceType.PERMISSION,
            SourceType.GROUP
    ));

    public void registerSourceType(final String sourceType) {
        sourceTypes.add(sourceType);
    }

    public Set<String> sourceTypes() {
        return new HashSet<>(sourceTypes);
    }
}

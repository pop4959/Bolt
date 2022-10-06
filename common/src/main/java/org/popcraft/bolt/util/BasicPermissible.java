package org.popcraft.bolt.util;

import java.util.Set;

public record BasicPermissible(String source) implements Permissible {
    @Override
    public Set<String> sources() {
        return Set.of(source);
    }
}

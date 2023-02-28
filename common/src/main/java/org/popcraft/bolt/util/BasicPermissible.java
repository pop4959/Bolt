package org.popcraft.bolt.util;

import java.util.Set;

public record BasicPermissible(Source source) implements Permissible {
    @Override
    public Set<Source> sources() {
        return Set.of(source);
    }
}

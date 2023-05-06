package org.popcraft.bolt.source;

import java.util.UUID;

@FunctionalInterface
public interface PlayerSourceResolver {
    boolean resolve(Source source, final UUID uuid);
}

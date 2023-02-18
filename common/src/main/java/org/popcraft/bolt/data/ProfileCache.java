package org.popcraft.bolt.data;

import java.util.UUID;

public interface ProfileCache {
    void load();

    void add(final UUID uuid, final String name);

    UUID getUniqueId(final String name);

    String getName(final UUID uuid);
}

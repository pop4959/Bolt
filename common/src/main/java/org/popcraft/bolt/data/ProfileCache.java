package org.popcraft.bolt.data;

import java.nio.file.Path;
import java.util.UUID;

public interface ProfileCache {
    void load(final Path path);

    void save(final Path path);

    void add(final UUID uuid, final String name);

    UUID getUniqueId(final String name);

    String getName(final UUID uuid);
}

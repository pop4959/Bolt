package org.popcraft.bolt.data;

import java.util.UUID;

public interface ProfileCache {
    void load();

    void add(final UUID uuid, final String name);

    Profile getProfile(final UUID uuid);

    Profile getProfile(final String name);
}

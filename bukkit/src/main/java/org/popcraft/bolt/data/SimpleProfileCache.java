package org.popcraft.bolt.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleProfileCache implements ProfileCache {
    public static final Profile EMPTY_PROFILE = new Profile(null, null);
    private final Path path;
    private final Map<UUID, String> uuidName = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameUuid = new ConcurrentHashMap<>();

    public SimpleProfileCache(final Path path) {
        this.path = path;
    }

    @Override
    public void load() {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (final BufferedReader reader = Files.newBufferedReader(path)) {
            reader.lines().forEach(line -> {
                final String[] split = line.split(":");
                final UUID uuid = UUID.fromString(split[0]);
                final String name = split[1];
                uuidName.put(uuid, name);
                nameUuid.put(name, uuid);
            });
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(final UUID uuid, final String name) {
        final UUID existingUUID = nameUuid.get(name);
        final String existingName = uuidName.get(uuid);
        if (uuid.equals(existingUUID) && name.equals(existingName)) {
            return;
        }
        uuidName.put(uuid, name);
        nameUuid.put(name, uuid);
        CompletableFuture.runAsync(() -> save(uuid, name));
    }

    private synchronized void save(final UUID uuid, final String name) {
        try (final Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(uuid + ":" + name + "\n");
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Profile getProfile(final UUID uuid) {
        return new Profile(uuid, getName(uuid));
    }

    @Override
    public Profile getProfile(final String name) {
        return new Profile(getUniqueId(name), name);
    }

    @Override
    public String getName(final UUID uuid) {
        return uuidName.get(uuid);
    }

    @Override
    public UUID getUniqueId(final String name) {
        return nameUuid.get(name);
    }
}

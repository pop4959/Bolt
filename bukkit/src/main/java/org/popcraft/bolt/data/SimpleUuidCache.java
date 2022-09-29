package org.popcraft.bolt.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleUuidCache implements UuidCache {
    private final Map<UUID, String> uuidName = new ConcurrentHashMap<>();
    private final Map<String, UUID> nameUuid = new ConcurrentHashMap<>();

    @Override
    public void load(final Path path) {
        if (Files.notExists(path)) {
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
    public void save(final Path path) {
        try (final BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (final Map.Entry<UUID, String> entry : uuidName.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue() + "\n");
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(final UUID uuid, final String name) {
        uuidName.put(uuid, name);
        nameUuid.put(name, uuid);
    }

    @Override
    public UUID getUniqueId(final String name) {
        return nameUuid.get(name);
    }

    @Override
    public String getName(final UUID uuid) {
        return uuidName.get(uuid);
    }
}

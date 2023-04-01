package org.popcraft.bolt.data.migration.lwc;

import com.google.gson.Gson;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.AccessList;
import org.popcraft.bolt.data.MemoryStore;
import org.popcraft.bolt.source.Source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class TrustMigration {
    private static final String DEFAULT_ACCESS_NORMAL = "normal";
    private final BoltPlugin plugin;

    public TrustMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public void convert() {
        if (!plugin.getBolt().getStore().loadAccessLists().join().isEmpty()) {
            return;
        }
        final Path lwcTrustDirectory = plugin.getPluginsPath().resolve("LWCTrust");
        if (!Files.exists(lwcTrustDirectory)) {
            return;
        }
        final Path trustsDirectory = lwcTrustDirectory.resolve("trusts");
        if (!Files.exists(trustsDirectory)) {
            return;
        }
        final MemoryStore store = new MemoryStore();
        final Gson gson = new Gson();
        try (final Stream<Path> trustFiles = Files.walk(trustsDirectory)) {
            trustFiles.filter(file -> !Files.isDirectory(file)).forEach(trustFile -> {
                try {
                    final String contents = Files.readString(trustFile);
                    final Trust trust = gson.fromJson(contents, Trust.class);
                    final UUID owner = trust.getOwner();
                    final List<UUID> trusted = trust.getTrusted();
                    final Map<String, String> access = new HashMap<>();
                    for (final UUID uuid : trusted) {
                        access.put(Source.player(uuid).toString(), DEFAULT_ACCESS_NORMAL);
                    }
                    final AccessList accessList = new AccessList(owner, access);
                    store.saveAccessList(accessList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (final AccessList accessList : store.loadAccessLists().join()) {
            plugin.getBolt().getStore().saveAccessList(accessList);
        }
    }
}

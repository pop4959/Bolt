package org.popcraft.bolt;

import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Permissible;
import org.popcraft.bolt.util.Source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Bolt {
    private final AccessRegistry accessRegistry = new AccessRegistry();
    private final Map<UUID, BoltPlayer> players = new HashMap<>();
    private Store store;

    public Bolt(final Store store) {
        this.store = store;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public BoltPlayer getPlayerMeta(final UUID uuid) {
        return players.computeIfAbsent(uuid, x -> new BoltPlayer(uuid));
    }

    public void removePlayerMeta(final UUID uuid) {
        players.remove(uuid);
    }

    public AccessRegistry getAccessRegistry() {
        return accessRegistry;
    }

    public boolean hasAccess(final Protection protection, final Permissible permissible, String... permissions) {
        final Set<String> sources = permissible.sources();
        final String ownerSource = Source.fromPlayer(protection.getOwner());
        if (sources.contains(ownerSource)) {
            return true;
        }
        final Set<String> heldPermissions = new HashSet<>();
        accessRegistry.get(protection.getType()).ifPresent(access -> heldPermissions.addAll(access.permissions()));
        protection.getAccess().forEach((source, accessType) -> {
            if (sources.contains(source)) {
                accessRegistry.get(accessType).ifPresent(access -> heldPermissions.addAll(access.permissions()));
            }
        });
        for (final String permission : permissions) {
            if (!heldPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }
}

package org.popcraft.bolt;

import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.Source;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Bolt {
    private final AccessRegistry accessRegistry = new AccessRegistry();
    private final Map<UUID, PlayerMeta> players = new HashMap<>();
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

    public PlayerMeta getPlayerMeta(final UUID uuid) {
        return players.computeIfAbsent(uuid, x -> new PlayerMeta(uuid));
    }

    public void removePlayerMeta(final UUID uuid) {
        players.remove(uuid);
    }

    public AccessRegistry getAccessRegistry() {
        return accessRegistry;
    }

    public boolean hasAccess(final Protection protection, final String source, String... permissions) {
        final String sourceType = Source.type(source);
        final String sourceIdentifier = Source.identifier(source);
        if (Source.PLAYER.equals(sourceType) && protection.getOwner().toString().equals(sourceIdentifier)) {
            return true;
        }
        final int numPermissionChecks = permissions.length;
        final boolean[] results = new boolean[numPermissionChecks];
        accessRegistry.get(protection.getType()).ifPresent(access -> {
            for (int i = 0; i < numPermissionChecks; ++i) {
                if (access.permissions().contains(permissions[i])) {
                    results[i] = true;
                }
            }
        });
        final String accessType = protection.getAccess().get(source);
        if (accessType != null) {
            accessRegistry.get(accessType).ifPresent(access -> {
                for (int i = 0; i < numPermissionChecks; ++i) {
                    if (access.permissions().contains(permissions[i])) {
                        results[i] = true;
                    }
                }
            });
        }
        for (boolean result : results) {
            if (!result) {
                return false;
            }
        }
        return true;
    }
}

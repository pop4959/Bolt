package org.popcraft.bolt;

import org.popcraft.bolt.util.defaults.DefaultAccess;
import org.popcraft.bolt.store.Store;
import org.popcraft.bolt.util.PlayerMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Bolt {
    private final AccessRegistry accessRegistry = new AccessRegistry();
    private final Map<UUID, PlayerMeta> players = new HashMap<>();
    private final AccessManager accessManager;
    private Store store;

    public Bolt(final Store store) {
        this();
        this.store = store;
        // TODO: replace this with proper registry
        this.accessRegistry.register(DefaultAccess.BASIC.type(), DefaultAccess.BASIC.access());
        this.accessRegistry.register(DefaultAccess.ADMIN.type(), DefaultAccess.ADMIN.access());
    }

    public Bolt() {
        this.accessManager = new AccessManager(this);
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public AccessRegistry getAccessRegistry() {
        return accessRegistry;
    }

    public AccessManager getAccessManager() {
        return accessManager;
    }

    public PlayerMeta getPlayerMeta(UUID uuid) {
        return players.computeIfAbsent(uuid, x -> new PlayerMeta(uuid));
    }
}

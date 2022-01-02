package org.popcraft.bolt;

import org.popcraft.bolt.data.store.Store;
import org.popcraft.bolt.registry.AccessRegistry;
import org.popcraft.bolt.util.BoltPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Bolt {
    private final AccessRegistry accessRegistry = new AccessRegistry();
    private final Map<UUID, BoltPlayer> players = new HashMap<>();
    private final AccessManager accessManager;
    private Store store;

    public Bolt(final Store store) {
        this();
        this.store = store;
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

    public BoltPlayer getBoltPlayer(UUID uuid) {
        return players.computeIfAbsent(uuid, x -> new BoltPlayer(uuid));
    }
}

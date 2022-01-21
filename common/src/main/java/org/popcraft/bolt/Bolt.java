package org.popcraft.bolt;

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
        this.accessManager = new AccessManager(this);
        this.store = store;
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

    public PlayerMeta getPlayerMeta(final UUID uuid) {
        return players.computeIfAbsent(uuid, x -> new PlayerMeta(uuid));
    }
}

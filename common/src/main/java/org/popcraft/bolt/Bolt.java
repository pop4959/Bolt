package org.popcraft.bolt;

import org.popcraft.bolt.access.AccessRegistry;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.source.PlayerSourceResolver;
import org.popcraft.bolt.source.SourceTypeRegistry;
import org.popcraft.bolt.util.BoltPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Bolt {
    private final AccessRegistry accessRegistry = new AccessRegistry();
    private final SourceTypeRegistry sourceTypeRegistry = new SourceTypeRegistry();
    private final Map<UUID, BoltPlayer> players = new HashMap<>();
    private final List<PlayerSourceResolver> registeredPlayerResolvers = new ArrayList<>();
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

    public BoltPlayer getBoltPlayer(final UUID uuid) {
        return players.computeIfAbsent(uuid, x -> new BoltPlayer(uuid));
    }

    public void removeBoltPlayer(final UUID uuid) {
        players.remove(uuid);
    }

    public AccessRegistry getAccessRegistry() {
        return accessRegistry;
    }

    public SourceTypeRegistry getSourceTypeRegistry() {
        return sourceTypeRegistry;
    }

    public List<PlayerSourceResolver> getRegisteredPlayerResolvers() {
        return registeredPlayerResolvers;
    }
}

package org.popcraft.bolt.util;

import org.popcraft.bolt.data.Source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerMeta {
    private final UUID uuid;
    private final Set<Action> actions = new HashSet<>();
    private final Map<Source, String> modifications = new HashMap<>();

    public PlayerMeta(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean hasAction(Action action) {
        return actions.contains(action);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public void removeAction(Action action) {
        actions.remove(action);
    }

    public Map<Source, String> getModifications() {
        return modifications;
    }
}

package org.popcraft.bolt.util;

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

    public boolean hasActions() {
        return !actions.isEmpty();
    }

    public boolean hasAction(Action action) {
        return actions.contains(action);
    }

    public void addAction(Action action) {
        actions.add(action);
    }

    public boolean triggerAction(Action action) {
        return actions.remove(action);
    }

    public Map<Source, String> getModifications() {
        return modifications;
    }
}

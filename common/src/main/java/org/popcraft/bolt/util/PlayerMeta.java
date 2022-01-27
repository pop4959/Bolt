package org.popcraft.bolt.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMeta {
    private final UUID uuid;
    private final Map<Source, String> modifications = new HashMap<>();
    private Action action;
    private boolean persist;

    public PlayerMeta(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean hasAction() {
        return this.action != null;
    }

    public boolean hasAction(Action action) {
        return action.equals(this.action);
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public boolean triggerAction(Action action) {
        final boolean triggered = hasAction(action);
        if (triggered && !this.persist) {
            this.action = null;
        }
        return triggered;
    }

    public boolean isPersist() {
        return persist;
    }

    public void togglePersist() {
        this.persist = !this.persist;
        if (!this.persist) {
            this.action = null;
        }
    }

    public Map<Source, String> getModifications() {
        return modifications;
    }
}

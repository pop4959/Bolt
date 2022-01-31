package org.popcraft.bolt.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMeta {
    private final UUID uuid;
    private final Map<Source, String> modifications = new HashMap<>();
    private Action action;
    private boolean interacted;
    private boolean persist;

    public PlayerMeta(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Action triggerAction() {
        final Action triggered = action;
        if (triggered != null && !this.persist) {
            this.action = null;
        }
        return triggered;
    }

    public boolean hasInteracted() {
        return interacted;
    }

    public void setInteracted() {
        this.interacted = true;
    }

    public void clearInteraction() {
        this.interacted = false;
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

package org.popcraft.bolt.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMeta {
    private final UUID uuid;
    private final Map<Source, String> modifications = new HashMap<>();
    private Action action;
    private Action lastAction;
    private boolean interacted;
    private boolean persist;
    private boolean lockNil;

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
            this.lastAction = triggered;
            this.action = null;
        }
        return triggered;
    }

    public boolean triggeredAction() {
        return lastAction != null;
    }

    public boolean hasInteracted() {
        return interacted;
    }

    public void setInteracted() {
        this.interacted = true;
    }

    public void clearInteraction() {
        this.lastAction = null;
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

    public boolean isLockNil() {
        return lockNil;
    }

    public void setLockNil(boolean lockNil) {
        this.lockNil = lockNil;
    }
}

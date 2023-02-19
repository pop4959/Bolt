package org.popcraft.bolt.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BoltPlayer implements Permissible {
    private final UUID uuid;
    private final Map<String, String> modifications = new HashMap<>();
    private final Set<String> sources = new HashSet<>();
    private final Set<Mode> modes = new HashSet<>();
    private Action action;
    private Action lastAction;
    private boolean interacted;
    private boolean lockNil;

    public BoltPlayer(UUID uuid) {
        this.uuid = uuid;
        this.sources.add(Source.fromPlayer(uuid));
    }

    public UUID getUuid() {
        return uuid;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void clearAction() {
        final Action triggered = action;
        if (triggered != null && !hasMode(Mode.PERSIST)) {
            this.lastAction = triggered;
            this.action = null;
        }
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

    public boolean hasMode(final Mode mode) {
        return this.modes.contains(mode);
    }

    public void toggleMode(final Mode mode) {
        if (this.modes.contains(mode)) {
            modes.remove(mode);
        } else {
            modes.add(mode);
        }
        if (!this.modes.contains(Mode.PERSIST)) {
            this.action = null;
        }
    }

    public Map<String, String> getModifications() {
        return modifications;
    }

    public boolean isLockNil() {
        return lockNil;
    }

    public void setLockNil(boolean lockNil) {
        this.lockNil = lockNil;
    }

    public void addPassword(String password) {
        sources.add(Source.fromPassword(password));
    }

    @Override
    public Set<String> sources() {
        return sources;
    }
}

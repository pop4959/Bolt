package org.popcraft.bolt.util;

import org.popcraft.bolt.source.Source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BoltPlayer {
    private final UUID uuid;
    private final Map<Source, String> modifications = new HashMap<>();
    private final Set<Source> sources = new HashSet<>();
    private final Set<Mode> modes = new HashSet<>();
    private Action action;
    private Action lastAction;
    private boolean interacted;
    private boolean trusting;
    private boolean trustingSilently;
    private boolean lockNil;

    public BoltPlayer(UUID uuid) {
        this.uuid = uuid;
        this.sources.add(Source.player(uuid));
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

    public Map<Source, String> getModifications() {
        return modifications;
    }

    public Map<Source, String> consumeModifications() {
        final Map<Source, String> modificationsCopy = new HashMap<>(modifications);
        if (!this.modes.contains(Mode.PERSIST)) {
            modifications.clear();
        }
        return modificationsCopy;
    }

    public boolean isTrusting() {
        return trusting;
    }

    public void setTrusting(boolean trusting) {
        this.trusting = trusting;
    }

    public boolean isTrustingSilently() {
        return trustingSilently;
    }

    public void setTrustingSilently(boolean trustingSilently) {
        this.trustingSilently = trustingSilently;
    }

    public boolean isLockNil() {
        return lockNil;
    }

    public void setLockNil(boolean lockNil) {
        this.lockNil = lockNil;
    }

    public void addPassword(String password) {
        sources.add(Source.password(password));
    }

    public Set<Source> sources() {
        return sources;
    }
}

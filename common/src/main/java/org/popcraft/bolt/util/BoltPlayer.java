package org.popcraft.bolt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BoltPlayer implements Permissible {
    private final UUID uuid;
    private final Map<String, String> modifications = new HashMap<>();
    private final Set<String> sources = new HashSet<>();
    private Action action;
    private Action lastAction;
    private boolean interacted;
    private boolean persist;
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
        if (triggered != null && !this.persist) {
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

    public boolean isPersist() {
        return persist;
    }

    public void togglePersist() {
        this.persist = !this.persist;
        if (!this.persist) {
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
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(password.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hash = new StringBuilder();
            for (final byte b : messageDigest.digest()) {
                hash.append("%02x".formatted(b));
            }
            sources.add(Source.from(Source.PASSWORD, hash.toString()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> sources() {
        return sources;
    }
}

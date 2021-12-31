package org.popcraft.bolt.util;

public enum Action {
    LOCK_BLOCK,
    LOCK_ENTITY,
    UNLOCK_BLOCK,
    UNLOCK_ENTITY;

    private final long timestamp;

    Action() {
        timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }
}

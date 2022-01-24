package org.popcraft.bolt.util;

public enum Action {
    LOCK,
    UNLOCK,
    INFO,
    DEBUG,
    MODIFY;

    private final long timestamp;

    Action() {
        timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }
}

package org.popcraft.bolt.util;

public class Action {
    private final Type type;
    private final String data;

    public Action(final Type type) {
        this.type = type;
        this.data = null;
    }

    public Action(final Type type, final String data) {
        this.type = type;
        this.data = data;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public enum Type {
        LOCK,
        UNLOCK,
        INFO,
        EDIT,
        DEBUG,
        TRANSFER,
    }
}

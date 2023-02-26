package org.popcraft.bolt.util;

public class Action {
    private final Type type;
    private final String data;
    private final boolean admin;

    public Action(final Type type) {
        this.type = type;
        this.data = null;
        this.admin = false;
    }

    public Action(final Type type, final String data) {
        this.type = type;
        this.data = data;
        this.admin = false;
    }

    public Action(Type type, String data, boolean admin) {
        this.type = type;
        this.data = data;
        this.admin = admin;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public boolean isAdmin() {
        return admin;
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

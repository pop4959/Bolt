package org.popcraft.bolt.util;

public class Action {
    private final Type type;
    private final String permission;
    private final String data;
    private final boolean admin;

    public Action(final Type type, final String permission) {
        this.type = type;
        this.permission = permission;
        this.data = null;
        this.admin = false;
    }

    public Action(final Type type, final String permission, final String data) {
        this.type = type;
        this.permission = permission;
        this.data = data;
        this.admin = false;
    }

    public Action(final Type type, final String permission, final String data, final boolean admin) {
        this.type = type;
        this.permission = permission;
        this.data = data;
        this.admin = admin;
    }

    public Type getType() {
        return type;
    }

    public String getPermission() {
        return permission;
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

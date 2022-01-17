package org.popcraft.bolt.data.defaults;

import org.popcraft.bolt.data.Access;
import org.popcraft.bolt.data.Permission;

import java.util.List;

public enum DefaultAccess {
    BASIC(new Access("basic", List.of(Permission.CONTAINER_ACCESS, Permission.CONTAINER_ADD, Permission.CONTAINER_REMOVE, Permission.INTERACT))),
    ADMIN(new Access("admin", List.of(Permission.BREAK, Permission.PLACE, Permission.CONTAINER_ACCESS, Permission.CONTAINER_ADD, Permission.CONTAINER_REMOVE, Permission.INTERACT)));

    private final Access access;

    DefaultAccess(final Access access) {
        this.access = access;
    }

    public Access access() {
        return access;
    }

    public String type() {
        return access.type();
    }

    public List<String> permissions() {
        return access.permissions();
    }
}

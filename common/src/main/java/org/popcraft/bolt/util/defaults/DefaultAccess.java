package org.popcraft.bolt.util.defaults;

import org.popcraft.bolt.util.Access;

import java.util.Set;

public enum DefaultAccess {
    BASIC(new Access("basic", Set.of(DefaultPermission.CONTAINER_ACCESS.getKey(), DefaultPermission.CONTAINER_ADD.getKey(), DefaultPermission.CONTAINER_REMOVE.getKey(), DefaultPermission.INTERACT.getKey()))),
    ADMIN(new Access("admin", Set.of(DefaultPermission.BREAK.getKey(), DefaultPermission.PLACE.getKey(), DefaultPermission.CONTAINER_ACCESS.getKey(), DefaultPermission.CONTAINER_ADD.getKey(), DefaultPermission.CONTAINER_REMOVE.getKey(), DefaultPermission.INTERACT.getKey())));

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

    public Set<String> permissions() {
        return access.permissions();
    }
}

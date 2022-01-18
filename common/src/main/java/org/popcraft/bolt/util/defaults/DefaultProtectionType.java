package org.popcraft.bolt.util.defaults;

import org.popcraft.bolt.util.Access;

import java.util.Collections;
import java.util.Set;

public enum DefaultProtectionType {
    PRIVATE(new Access("private", Collections.emptySet())),
    DEPOSIT(new Access("deposit", Set.of(DefaultPermission.CONTAINER_ACCESS.getKey(), DefaultPermission.CONTAINER_ADD.getKey()))),
    WITHDRAWAL(new Access("withdrawal", Set.of(DefaultPermission.CONTAINER_ACCESS.getKey(), DefaultPermission.CONTAINER_REMOVE.getKey()))),
    DISPLAY(new Access("display", Set.of(DefaultPermission.CONTAINER_ACCESS.getKey()))),
    PUBLIC(new Access("public", Set.of(DefaultPermission.PLACE.getKey(), DefaultPermission.CONTAINER_ACCESS.getKey(), DefaultPermission.CONTAINER_ADD.getKey(), DefaultPermission.CONTAINER_REMOVE.getKey(), DefaultPermission.INTERACT.getKey())));

    private final Access access;

    DefaultProtectionType(final Access access) {
        this.access = access;
    }

    public String type() {
        return access.type();
    }

    public Set<String> permissions() {
        return access.permissions();
    }
}

package org.popcraft.bolt.data.protection;

import org.popcraft.bolt.data.Access;
import org.popcraft.bolt.data.Permission;

import java.util.Collections;
import java.util.List;

public enum DefaultProtectionType {
    PRIVATE(new Access("private", Collections.emptyList())),
    DEPOSIT(new Access("deposit", List.of(Permission.CONTAINER_ACCESS, Permission.CONTAINER_ADD))),
    WITHDRAWAL(new Access("withdrawal", List.of(Permission.CONTAINER_ACCESS, Permission.CONTAINER_REMOVE))),
    DISPLAY(new Access("display", List.of(Permission.CONTAINER_ACCESS))),
    PUBLIC(new Access("public", List.of(Permission.PLACE, Permission.CONTAINER_ACCESS, Permission.CONTAINER_ADD, Permission.CONTAINER_REMOVE, Permission.INTERACT)));

    private final Access access;

    DefaultProtectionType(final Access access) {
        this.access = access;
    }

    public String type() {
        return access.type();
    }

    public List<String> permissions() {
        return access.permissions();
    }
}

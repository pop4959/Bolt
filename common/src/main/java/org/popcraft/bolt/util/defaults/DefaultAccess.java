package org.popcraft.bolt.util.defaults;

import org.popcraft.bolt.util.Access;
import org.popcraft.bolt.util.Permission;

import java.util.Set;

public enum DefaultAccess {
    BASIC(new Access("basic", Set.of(Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT, Permission.WITHDRAW))),
    FULL(new Access("full", Set.of(Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT, Permission.WITHDRAW, Permission.DESTROY, Permission.MODIFY, Permission.EDIT)));

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

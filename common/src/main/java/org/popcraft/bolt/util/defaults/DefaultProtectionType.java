package org.popcraft.bolt.util.defaults;

import org.popcraft.bolt.util.Access;
import org.popcraft.bolt.util.Permission;

import java.util.Collections;
import java.util.Set;

public enum DefaultProtectionType {
    PRIVATE(new Access("private", Collections.emptySet())),
    DISPLAY(new Access("display", Set.of(Permission.INTERACT, Permission.OPEN))),
    DEPOSIT(new Access("deposit", Set.of(Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT))),
    WITHDRAWAL(new Access("withdrawal", Set.of(Permission.INTERACT, Permission.OPEN, Permission.WITHDRAW))),
    PUBLIC(new Access("public", Set.of(Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT, Permission.WITHDRAW)));

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

package org.popcraft.bolt.access;

import org.popcraft.bolt.util.Permission;

import java.util.Set;

public final class DefaultAccess {
    private DefaultAccess() {
    }

    public static final Set<String> PRIVATE = Set.of(Permission.REDSTONE);
    public static final Set<String> DISPLAY = Set.of(Permission.REDSTONE, Permission.INTERACT, Permission.OPEN);
    public static final Set<String> DEPOSIT = Set.of(Permission.REDSTONE, Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT);
    public static final Set<String> WITHDRAWAL = Set.of(Permission.REDSTONE, Permission.INTERACT, Permission.OPEN, Permission.WITHDRAW);
    public static final Set<String> PUBLIC = Set.of(Permission.REDSTONE, Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT, Permission.WITHDRAW, Permission.MOUNT);
    public static final Set<String> NORMAL = Set.of(Permission.REDSTONE, Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT, Permission.WITHDRAW, Permission.MOUNT);
    public static final Set<String> ADMIN = Set.of(Permission.REDSTONE, Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT, Permission.WITHDRAW, Permission.MOUNT, Permission.EDIT);
    public static final Set<String> OWNER = Set.of(Permission.REDSTONE, Permission.INTERACT, Permission.OPEN, Permission.DEPOSIT, Permission.WITHDRAW, Permission.MOUNT, Permission.EDIT, Permission.DESTROY);
}

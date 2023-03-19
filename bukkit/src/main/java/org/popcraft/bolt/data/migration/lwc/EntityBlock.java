package org.popcraft.bolt.data.migration.lwc;

import org.popcraft.bolt.protection.BlockProtection;

import java.util.UUID;

public class EntityBlock {
    private EntityBlock() {
    }

    public static boolean check(final BlockProtection blockProtection) {
        return "AIR".equals(blockProtection.getBlock())
                && blockProtection.getX() == blockProtection.getY()
                && blockProtection.getY() == blockProtection.getZ();
    }

    public static int magic(final UUID uuid) {
        return 50000 + uuid.hashCode();
    }
}

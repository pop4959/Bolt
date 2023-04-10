package org.popcraft.bolt.util;

import org.popcraft.bolt.protection.BlockProtection;

public record BlockLocation(String world, int x, int y, int z) {
    public static BlockLocation fromProtection(final BlockProtection blockProtection) {
        return new BlockLocation(blockProtection.getWorld(), blockProtection.getX(), blockProtection.getY(), blockProtection.getZ());
    }
}

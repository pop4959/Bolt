package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import static org.popcraft.bolt.util.lang.Translator.translate;

public final class Protections {
    private Protections() {
    }

    @SuppressWarnings("java:S2583")
    public static String displayType(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            final World world = Bukkit.getWorld(blockProtection.getWorld());
            final int x = blockProtection.getX();
            final int y = blockProtection.getY();
            final int z = blockProtection.getZ();
            if (world == null || !world.isChunkLoaded(x >> 4, z >> 4)) {
                return Strings.toTitleCase(blockProtection.getBlock());
            } else {
                final Block block = world.getBlockAt(x, y, z);
                if (block.getState() instanceof final Nameable nameable && nameable.getCustomName() != null) {
                    return nameable.getCustomName();
                } else {
                    return displayType(block);
                }
            }
        } else if (protection instanceof final EntityProtection entityProtection) {
            final Entity entity = Bukkit.getServer().getEntity(entityProtection.getId());
            if (entity == null) {
                return Strings.toTitleCase(entityProtection.getEntity());
            } else {
                return displayType(entity);
            }
        } else {
            return translate(Translation.UNKNOWN);
        }
    }

    public static String displayType(final Block block) {
        return Strings.toTitleCase(block.getType().toString());
    }

    public static String displayType(final Entity entity) {
        return entity.getName();
    }
}

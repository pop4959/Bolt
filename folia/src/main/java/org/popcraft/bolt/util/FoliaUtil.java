package org.popcraft.bolt.util;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class FoliaUtil {
    private static final boolean CONFIG_EXISTS = classExists("io.papermc.paper.threadedregions.RegionizedServer");

    private FoliaUtil() {
    }

    public static boolean isFolia() {
        return CONFIG_EXISTS;
    }

    public static Collection<Entity> getNearbyEntities(final Block block, final BoundingBox boundingBox, final Predicate<Entity> filter) {
        final World world = block.getWorld();
        if (isFolia()) {
            final int minChunkX = boundingBox.getMin().getBlockX() >> 4;
            final int maxChunkX = boundingBox.getMax().getBlockX() >> 4;
            final int minChunkZ = boundingBox.getMin().getBlockZ() >> 4;
            final int maxChunkZ = boundingBox.getMax().getBlockZ() >> 4;
            final List<Entity> nearbyEntities = new ArrayList<>();
            for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }
                    final Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    if (!chunk.isEntitiesLoaded()) {
                        continue;
                    }
                    for (final Entity entity : chunk.getEntities()) {
                        if ((filter == null || filter.test(entity)) && boundingBox.overlaps(entity.getBoundingBox())) {
                            nearbyEntities.add(entity);
                        }
                    }
                }
            }
            return nearbyEntities;
        } else {
            return world.getNearbyEntities(boundingBox, filter);
        }
    }

    private static boolean classExists(final String clazz) {
        try {
            Class.forName(clazz);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

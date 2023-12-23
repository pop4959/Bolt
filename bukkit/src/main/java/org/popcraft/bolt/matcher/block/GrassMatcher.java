package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class GrassMatcher implements BlockMatcher {
    private static final Material SHORT_GRASS_OLD = EnumUtil.valueOf(Material.class, "GRASS").orElse(null);
    private static final Material SHORT_GRASS = EnumUtil.valueOf(Material.class, "SHORT_GRASS").orElse(null);
    private static final EnumSet<Material> GRASS = EnumSet.of(Material.FERN, Material.SEAGRASS, Material.NETHER_SPROUTS, Material.WARPED_ROOTS, Material.CRIMSON_ROOTS);
    private boolean enabled;

    static {
        // Future: Replace with Material.SHORT_GRASS
        if (SHORT_GRASS_OLD != null) {
            GRASS.add(SHORT_GRASS_OLD);
        }
        if (SHORT_GRASS != null) {
            GRASS.add(SHORT_GRASS);
        }
    }

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(GRASS::contains);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean canMatch(Block block) {
        return enabled;
    }

    @Override
    public Match findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        if (GRASS.contains(above.getType())) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

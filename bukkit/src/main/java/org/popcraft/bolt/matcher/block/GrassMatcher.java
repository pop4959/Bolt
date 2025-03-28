package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GrassMatcher implements BlockMatcher {
    private static final Material BUSH = EnumUtil.valueOf(Material.class, "BUSH").orElse(null);
    private static final Material FIREFLY_BUSH = EnumUtil.valueOf(Material.class, "FIREFLY_BUSH").orElse(null);
    private static final Material SHORT_DRY_GRASS = EnumUtil.valueOf(Material.class, "SHORT_DRY_GRASS").orElse(null);
    private static final Material TALL_DRY_GRASS = EnumUtil.valueOf(Material.class, "TALL_DRY_GRASS").orElse(null);
    // Future: make immutable
    private static final Set<Material> GRASS = new HashSet<>(Set.of(Material.SHORT_GRASS, Material.FERN, Material.SEAGRASS, Material.NETHER_SPROUTS, Material.WARPED_ROOTS, Material.CRIMSON_ROOTS));
    private boolean enabled;

    static {
        // Future: Replace with Material.BUSH
        if (BUSH != null) {
            GRASS.add(BUSH);
        }
        // Future: Replace with Material.FIREFLY_BUSH
        if (FIREFLY_BUSH != null) {
            GRASS.add(FIREFLY_BUSH);
        }
        // Future: Replace with Material.SHORT_DRY_GRASS
        if (SHORT_DRY_GRASS != null) {
            GRASS.add(SHORT_DRY_GRASS);
        }
        // Future: Replace with Material.TALL_DRY_GRASS
        if (TALL_DRY_GRASS != null) {
            GRASS.add(TALL_DRY_GRASS);
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

package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.Set;

public class GrassMatcher implements BlockMatcher {
    private static final Set<Material> GRASS = Set.of(Material.SHORT_GRASS, Material.FERN, Material.SEAGRASS, Material.NETHER_SPROUTS, Material.WARPED_ROOTS, Material.CRIMSON_ROOTS);
    private boolean enabled;

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

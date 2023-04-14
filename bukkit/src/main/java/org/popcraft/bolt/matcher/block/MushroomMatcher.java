package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class MushroomMatcher implements BlockMatcher {
    private static final EnumSet<Material> MUSHROOMS = EnumSet.of(Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.WARPED_FUNGUS, Material.CRIMSON_FUNGUS);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(MUSHROOMS::contains);
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
        if (MUSHROOMS.contains(above.getType())) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

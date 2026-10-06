package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class ShelfMushroomMatcher implements BlockMatcher {
    private static final Material SHELF_MUSHROOM = EnumUtil.valueOf(Material.class, "SHELF_MUSHROOM").orElse(null);
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.contains(SHELF_MUSHROOM);
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
        for (final BlockFace blockFace : CARDINAL_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getType().equals(SHELF_MUSHROOM) && adjacent.getBlockData() instanceof final Directional directional && blockFace.equals(directional.getFacing())) {
                return Match.ofBlocks(Collections.singleton(adjacent));
            }
        }
        return null;
    }
}

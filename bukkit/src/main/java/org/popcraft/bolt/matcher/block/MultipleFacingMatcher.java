package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class MultipleFacingMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARTESIAN_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
    // Future: make immutable
    private static final Set<Material> MULTIPLE_FACING = new HashSet<>(Set.of(Material.SCULK_VEIN, Material.GLOW_LICHEN));
    private static final Material RESIN_CLUMP = EnumUtil.valueOf(Material.class, "RESIN_CLUMP").orElse(null);
    private boolean enabled;

    static {
        // Future: Replace with Material.RESIN_CLUMP
        if (RESIN_CLUMP != null) {
            MULTIPLE_FACING.add(RESIN_CLUMP);
        }
    }

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(MULTIPLE_FACING::contains);
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
        final Set<Block> blocks = new HashSet<>();
        for (final BlockFace blockFace : CARTESIAN_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (MULTIPLE_FACING.contains(adjacent.getType()) && adjacent.getBlockData() instanceof final MultipleFacing multipleFacing && multipleFacing.getFaces().contains(blockFace.getOppositeFace())) {
                blocks.add(adjacent);
            }
        }
        return Match.ofBlocks(blocks);
    }
}

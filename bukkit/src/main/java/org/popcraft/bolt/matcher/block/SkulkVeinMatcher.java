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
import java.util.Optional;
import java.util.Set;

public class SkulkVeinMatcher implements BlockMatcher {
    private static final Material SCULK_VEIN = EnumUtil.valueOf(Material.class, "SCULK_VEIN").orElse(null);
    private static final EnumSet<BlockFace> CARTESIAN_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        // Future: Replace with Material.SCULK_VEIN
        if (SCULK_VEIN == null) {
            enabled = false;
        } else {
            enabled = protectableBlocks.contains(SCULK_VEIN);
        }
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
    public Optional<Match> findMatch(Block block) {
        final Set<Block> blocks = new HashSet<>();
        for (final BlockFace blockFace : CARTESIAN_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getType().equals(SCULK_VEIN) && adjacent.getBlockData() instanceof final MultipleFacing multipleFacing && multipleFacing.getFaces().contains(blockFace.getOppositeFace())) {
                blocks.add(adjacent);
            }
        }
        return Optional.of(Match.ofBlocks(blocks));
    }
}

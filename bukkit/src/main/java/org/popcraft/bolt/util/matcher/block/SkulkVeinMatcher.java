package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.popcraft.bolt.util.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class SkulkVeinMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARTESIAN_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Set<Block> blocks = new HashSet<>();
        for (final BlockFace blockFace : CARTESIAN_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (Material.SCULK_VEIN.equals(adjacent.getType()) && adjacent.getBlockData() instanceof final MultipleFacing multipleFacing && multipleFacing.getFaces().contains(blockFace.getOppositeFace())) {
                blocks.add(adjacent);
            }
        }
        return Optional.of(Match.ofBlocks(blocks));
    }
}

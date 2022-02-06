package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Scaffolding;
import org.popcraft.bolt.util.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ScaffoldingMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> SUPPORT_FACES = EnumSet.of(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (Material.SCAFFOLDING.equals(block.getType()) || Material.SCAFFOLDING.equals(block.getRelative(BlockFace.UP).getType())) {
            final Set<Block> blocks = new HashSet<>();
            findScaffolding(blocks, block);
            return Optional.of(Match.ofBlocks(blocks));
        }
        return Optional.empty();
    }

    private void findScaffolding(final Set<Block> scaffolds, final Block current) {
        for (final BlockFace blockFace : SUPPORT_FACES) {
            final Block adjacent = current.getRelative(blockFace);
            if (adjacent.getBlockData() instanceof final Scaffolding adjacentScaffolding && (BlockFace.UP.equals(blockFace) || (current.getBlockData() instanceof final Scaffolding currentScaffolding && adjacentScaffolding.isBottom() && adjacentScaffolding.getDistance() > currentScaffolding.getDistance()))) {
                scaffolds.add(adjacent);
                findScaffolding(scaffolds, adjacent);
            }
        }
    }
}

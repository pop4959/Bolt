package org.popcraft.bolt.util.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Fire;
import org.popcraft.bolt.util.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FireMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> FIRE_FACES = EnumSet.of(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Set<Block> blocks = new HashSet<>();
        for (final BlockFace blockFace : FIRE_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getBlockData() instanceof final Fire fire && ((BlockFace.UP.equals(blockFace) && fire.getFaces().isEmpty()) || fire.getFaces().contains(blockFace.getOppositeFace()))) {
                blocks.add(adjacent);
            }
        }
        return Optional.of(Match.ofBlocks(blocks));
    }
}

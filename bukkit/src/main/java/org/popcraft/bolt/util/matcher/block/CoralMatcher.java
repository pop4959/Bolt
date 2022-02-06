package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.CoralWallFan;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public class CoralMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        for (final BlockFace blockFace : CARDINAL_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getBlockData() instanceof final CoralWallFan coralWallFan && blockFace.equals(coralWallFan.getFacing())) {
                return Optional.of(Match.ofBlocks(Collections.singleton(adjacent)));
            }
        }
        final Block above = block.getRelative(BlockFace.UP);
        if (Tag.CORALS.isTagged(above.getType()) && !(above.getBlockData() instanceof CoralWallFan)) {
            return Optional.of(Match.ofBlocks(Collections.singleton(above)));
        }
        return Optional.empty();
    }
}

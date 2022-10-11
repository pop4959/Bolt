package org.popcraft.bolt.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public class SignMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        if (above.getBlockData() instanceof Sign) {
            return Optional.of(Match.ofBlocks(Collections.singleton(above)));
        }
        for (final BlockFace blockFace : CARDINAL_FACES) {
            final Block wall = block.getRelative(blockFace);
            if (wall.getBlockData() instanceof final WallSign wallSign && blockFace.equals(wallSign.getFacing())) {
                return Optional.of(Match.ofBlocks(Collections.singleton(wall)));
            }
        }
        return Optional.empty();
    }
}

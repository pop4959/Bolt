package org.popcraft.bolt.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.popcraft.bolt.matcher.Match;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DoorMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Set<Block> blocks = new HashSet<>();
        if (block.getBlockData() instanceof Door door) {
            if (Bisected.Half.BOTTOM.equals(door.getHalf())) {
                final Block upperHalf = block.getRelative(BlockFace.UP);
                if (upperHalf.getBlockData() instanceof Door) {
                    blocks.add(upperHalf);
                }
            } else {
                final Block lowerHalf = block.getRelative(BlockFace.DOWN);
                if (lowerHalf.getBlockData() instanceof Door) {
                    blocks.add(lowerHalf);
                }
            }
        } else {
            final Block lowerHalf = block.getRelative(BlockFace.UP);
            if (lowerHalf.getBlockData() instanceof Door) {
                blocks.add(lowerHalf);
            }
            final Block upperHalf = lowerHalf.getRelative(BlockFace.UP);
            if (upperHalf.getBlockData() instanceof Door) {
                blocks.add(upperHalf);
            }
        }
        return Optional.of(Match.ofBlocks(blocks));
    }
}

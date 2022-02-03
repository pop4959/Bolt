package org.popcraft.bolt.util.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bell;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class BellMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARTESIAN_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        for (final BlockFace blockFace : CARTESIAN_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getBlockData() instanceof final Bell bell) {
                if (Bell.Attachment.CEILING.equals(bell.getAttachment()) && BlockFace.DOWN.equals(blockFace)) {
                    return Optional.of(Match.ofBlocks(Collections.singleton(adjacent)));
                } else if (Bell.Attachment.FLOOR.equals(bell.getAttachment()) && BlockFace.UP.equals(blockFace)) {
                    return Optional.of(Match.ofBlocks(Collections.singleton(adjacent)));
                } else if (Bell.Attachment.SINGLE_WALL.equals(bell.getAttachment()) && blockFace.getOppositeFace().equals(bell.getFacing())) {
                    return Optional.of(Match.ofBlocks(Collections.singleton(adjacent)));
                } else if (Bell.Attachment.DOUBLE_WALL.equals(bell.getAttachment())) {
                    return Optional.of(Match.ofBlocks(List.of(block.getRelative(bell.getFacing()), block.getRelative(bell.getFacing().getOppositeFace()))));
                }
            }
        }
        return Optional.empty();
    }
}

package org.popcraft.bolt.util.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public class SwitchMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARTESIAN_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        for (final BlockFace blockFace : CARTESIAN_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getBlockData() instanceof final Switch zwitch && ((FaceAttachable.AttachedFace.CEILING.equals(zwitch.getAttachedFace()) && BlockFace.DOWN.equals(blockFace)) || (FaceAttachable.AttachedFace.FLOOR.equals(zwitch.getAttachedFace()) && BlockFace.UP.equals(blockFace)) || (zwitch.getFacing().equals(blockFace)))) {
                return Optional.of(Match.ofBlocks(Collections.singleton(adjacent)));
            }
        }
        return Optional.empty();
    }
}

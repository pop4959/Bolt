package org.popcraft.bolt.matcher.block;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TallFlowerMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (Tag.TALL_FLOWERS.isTagged(block.getType()) && block.getBlockData() instanceof final Bisected bisected) {
            final Bisected.Half half = bisected.getHalf();
            if (Bisected.Half.BOTTOM.equals(half)) {
                return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(BlockFace.UP))));
            } else {
                return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(BlockFace.DOWN))));
            }
        } else {
            final Block above = block.getRelative(BlockFace.UP);
            if (Tag.TALL_FLOWERS.isTagged(above.getType())) {
                return Optional.of(Match.ofBlocks(List.of(above, above.getRelative(BlockFace.UP))));
            }
        }
        return Optional.empty();
    }
}

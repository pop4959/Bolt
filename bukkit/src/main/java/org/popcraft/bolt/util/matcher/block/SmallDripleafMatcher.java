package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SmallDripleafMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (Material.SMALL_DRIPLEAF.equals(block.getType()) && block.getBlockData() instanceof final Bisected bisected) {
            final Bisected.Half half = bisected.getHalf();
            if (Bisected.Half.BOTTOM.equals(half)) {
                return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(BlockFace.UP))));
            } else {
                return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(BlockFace.DOWN))));
            }
        } else {
            final Block above = block.getRelative(BlockFace.UP);
            if (Material.SMALL_DRIPLEAF.equals(above.getType())) {
                return Optional.of(Match.ofBlocks(List.of(above, above.getRelative(BlockFace.UP))));
            }
        }
        return Optional.empty();
    }
}

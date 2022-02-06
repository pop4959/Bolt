package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.Optional;

public class HangingRootsMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Block below = block.getRelative(BlockFace.DOWN);
        if (Material.HANGING_ROOTS.equals(below.getType())) {
            return Optional.of(Match.ofBlocks(Collections.singleton(below)));
        }
        return Optional.empty();
    }
}

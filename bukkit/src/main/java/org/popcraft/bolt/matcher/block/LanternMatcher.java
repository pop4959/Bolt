package org.popcraft.bolt.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Lantern;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.Optional;

public class LanternMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        if (above.getBlockData() instanceof final Lantern lantern && !lantern.isHanging()) {
            return Optional.of(Match.ofBlocks(Collections.singleton(above)));
        }
        final Block below = block.getRelative(BlockFace.DOWN);
        if (below.getBlockData() instanceof final Lantern lantern && lantern.isHanging()) {
            return Optional.of(Match.ofBlocks(Collections.singleton(below)));
        }
        return Optional.empty();
    }
}

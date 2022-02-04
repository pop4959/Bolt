package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.TechnicalPiston;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.Optional;

public class TechnicalPistonMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return Material.PISTON_HEAD.equals(block.getType());
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (block.getBlockData() instanceof final TechnicalPiston technicalPiston) {
            return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(technicalPiston.getFacing().getOppositeFace()))));
        }
        return Optional.empty();
    }
}

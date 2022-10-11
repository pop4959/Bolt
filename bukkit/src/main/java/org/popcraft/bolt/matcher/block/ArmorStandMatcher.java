package org.popcraft.bolt.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.popcraft.bolt.matcher.Match;

import java.util.Optional;

public class ArmorStandMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        return Optional.of(Match.ofEntities(block.getWorld().getNearbyEntities(block.getBoundingBox().expand(0, 0, 0, 0, 1, 0), ArmorStand.class::isInstance)));
    }
}

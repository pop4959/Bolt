package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.LeashHitch;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Optional;

public class LeashHitchMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return Tag.FENCES.isTagged(block.getType());
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        return Optional.of(Match.ofEntities(block.getWorld().getNearbyEntities(block.getBoundingBox(), LeashHitch.class::isInstance)));
    }
}

package org.popcraft.bolt.util.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Optional;

public class ArmorStandMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        return Optional.of(Match.ofEntities(above.getWorld().getNearbyEntities(above.getBoundingBox(), ArmorStand.class::isInstance)));
    }
}

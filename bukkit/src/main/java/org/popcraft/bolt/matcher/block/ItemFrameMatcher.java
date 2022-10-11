package org.popcraft.bolt.matcher.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.popcraft.bolt.matcher.Match;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ItemFrameMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Set<Entity> entities = new HashSet<>();
        block.getWorld().getNearbyEntities(block.getBoundingBox().expand(0.5, 0.5, 0.5, 0.5, 0.5, 0.5), ItemFrame.class::isInstance).forEach(entity -> {
            if (entity instanceof final ItemFrame itemFrame && itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace()).getLocation().equals(block.getLocation())) {
                entities.add(entity);
            }
        });
        return Optional.of(Match.ofEntities(entities));
    }
}

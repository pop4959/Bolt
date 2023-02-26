package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LeashHitch;
import org.popcraft.bolt.matcher.Match;

import java.util.Optional;
import java.util.Set;

public class LeashHitchMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableEntities.contains(EntityType.LEASH_HITCH);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean canMatch(Block block) {
        return enabled && Tag.FENCES.isTagged(block.getType());
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        return Optional.of(Match.ofEntities(block.getWorld().getNearbyEntities(block.getBoundingBox(), LeashHitch.class::isInstance)));
    }
}

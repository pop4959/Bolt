package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Optional;
import java.util.Set;

public class ArmorStandMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableEntities.contains(EntityType.ARMOR_STAND);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean canMatch(Block block) {
        return enabled;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        return Optional.of(Match.ofEntities(block.getWorld().getNearbyEntities(block.getBoundingBox().expand(0, 0, 0, 0, 1, 0), ArmorStand.class::isInstance)));
    }
}

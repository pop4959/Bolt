package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.Set;

public class RepeaterMatcher implements BlockMatcher {
    private static final Set<Material> REPEATERS = Set.of(Material.REPEATER, Material.COMPARATOR);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(REPEATERS::contains);
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
    public Match findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        if (REPEATERS.contains(above.getType())) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

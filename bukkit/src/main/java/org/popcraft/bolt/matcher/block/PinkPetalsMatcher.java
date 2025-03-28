package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PinkPetalsMatcher implements BlockMatcher {
    private static final Material LEAF_LITTER = EnumUtil.valueOf(Material.class, "LEAF_LITTER").orElse(null);
    private static final Material WILDFLOWERS = EnumUtil.valueOf(Material.class, "WILDFLOWERS").orElse(null);
    // Future: make immutable
    private static final Set<Material> PETALS = new HashSet<>(Set.of(Material.PINK_PETALS));
    private boolean enabled;

    static {
        // Future: Replace with Material.LEAF_LITTER
        if (LEAF_LITTER != null) {
            PETALS.add(LEAF_LITTER);
        }
        // Future: Replace with Material.WILDFLOWERS
        if (WILDFLOWERS != null) {
            PETALS.add(WILDFLOWERS);
        }
    }

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(PETALS::contains);
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
        if (PETALS.contains(above.getType())) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.Set;

public class PinkPetalsMatcher implements BlockMatcher {
    private static final Material PINK_PETALS = EnumUtil.valueOf(Material.class, "PINK_PETALS").orElse(null);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        // Future: Replace with Material.PINK_PETALS
        if (PINK_PETALS == null) {
            enabled = false;
        } else {
            enabled = protectableBlocks.contains(PINK_PETALS);
        }
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
        if (above.getType().equals(PINK_PETALS)) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

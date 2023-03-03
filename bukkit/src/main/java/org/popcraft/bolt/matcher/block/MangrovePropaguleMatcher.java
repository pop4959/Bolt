package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class MangrovePropaguleMatcher implements BlockMatcher {
    private static final Material MANGROVE_PROPAGULE = EnumUtil.valueOf(Material.class, "MANGROVE_PROPAGULE").orElse(null);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        // Future: Replace with Material.MANGROVE_PROPAGULE
        if (MANGROVE_PROPAGULE == null) {
            enabled = false;
        } else {
            enabled = protectableBlocks.contains(MANGROVE_PROPAGULE);
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
    public Optional<Match> findMatch(Block block) {
        final Block below = block.getRelative(BlockFace.DOWN);
        if (below.getType().equals(MANGROVE_PROPAGULE)) {
            return Optional.of(Match.ofBlocks(Collections.singleton(below)));
        }
        return Optional.empty();
    }
}

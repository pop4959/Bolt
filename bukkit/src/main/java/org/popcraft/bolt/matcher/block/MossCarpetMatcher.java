package org.popcraft.bolt.matcher.block;

import com.google.common.collect.Sets;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.Set;

public class MossCarpetMatcher implements BlockMatcher {
    // Future: make immutable (Set.of)
    private static final Set<Material> MOSS_CARPETS = Sets.newHashSet(Material.MOSS_CARPET);
    private static final Material PALE_MOSS_CARPET = EnumUtil.valueOf(Material.class, "PALE_MOSS_CARPET").orElse(null);
    private boolean enabled;

    static {
        // Future: Replace with Material.PALE_MOSS_CARPET
        if (PALE_MOSS_CARPET != null) {
            MOSS_CARPETS.add(PALE_MOSS_CARPET);
        }
    }

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(MOSS_CARPETS::contains);
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
        if (MOSS_CARPETS.contains(above.getType())) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.Set;

public class SmallFlowerMatcher implements BlockMatcher {
    // Future: Replace with Material.CACTUS_FLOWER
    private static final Material CACTUS_FLOWER = EnumUtil.valueOf(Material.class, "CACTUS_FLOWER").orElse(null);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> Tag.SMALL_FLOWERS.isTagged(material) || material.equals(CACTUS_FLOWER));
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
        if (Tag.SMALL_FLOWERS.isTagged(above.getType()) || above.getType().equals(CACTUS_FLOWER)) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

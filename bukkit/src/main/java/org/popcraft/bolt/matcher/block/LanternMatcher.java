package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.Set;

public class LanternMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> material.createBlockData() instanceof Lantern);
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
        if (above.getBlockData() instanceof final Lantern lantern && !lantern.isHanging()) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        final Block below = block.getRelative(BlockFace.DOWN);
        if (below.getBlockData() instanceof final Lantern lantern && lantern.isHanging()) {
            return Match.ofBlocks(Collections.singleton(below));
        }
        return null;
    }
}

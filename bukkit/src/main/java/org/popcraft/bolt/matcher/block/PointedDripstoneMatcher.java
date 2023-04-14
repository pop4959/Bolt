package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.HashSet;
import java.util.Set;

public class PointedDripstoneMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> material.createBlockData() instanceof PointedDripstone);
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
        if (block.getBlockData() instanceof final PointedDripstone pointedDripstone) {
            final Set<Block> blocks = new HashSet<>();
            final BlockFace verticalDirection = pointedDripstone.getVerticalDirection();
            for (Block next = block.getRelative(verticalDirection); next.getBlockData() instanceof final PointedDripstone nextDripstone && verticalDirection.equals(nextDripstone.getVerticalDirection()); next = next.getRelative(verticalDirection)) {
                blocks.add(next);
            }
            return Match.ofBlocks(blocks);
        }
        final Block above = block.getRelative(BlockFace.UP);
        if (above.getBlockData() instanceof final PointedDripstone pointedDripstone && BlockFace.UP.equals(pointedDripstone.getVerticalDirection())) {
            final Set<Block> blocks = new HashSet<>();
            for (Block next = above; next.getBlockData() instanceof final PointedDripstone nextDripstone && BlockFace.UP.equals(nextDripstone.getVerticalDirection()); next = next.getRelative(BlockFace.UP)) {
                blocks.add(next);
            }
            return Match.ofBlocks(blocks);
        }
        final Block below = block.getRelative(BlockFace.DOWN);
        if (below.getBlockData() instanceof final PointedDripstone pointedDripstone && BlockFace.DOWN.equals(pointedDripstone.getVerticalDirection())) {
            final Set<Block> blocks = new HashSet<>();
            for (Block next = below; next.getBlockData() instanceof final PointedDripstone nextDripstone && BlockFace.DOWN.equals(nextDripstone.getVerticalDirection()); next = next.getRelative(BlockFace.DOWN)) {
                blocks.add(next);
            }
            return Match.ofBlocks(blocks);
        }
        return null;
    }
}

package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Speleothem;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.HashSet;
import java.util.Set;

public class SpeleothemMatcher implements BlockMatcher {
    private boolean enabled;

    private static final boolean CAN_USE = classExists();

    private static boolean classExists() {
        try {
            Class.forName("org.bukkit.block.data.type.Speleothem");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean canUse() {
        return CAN_USE;
    }

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        if (!canUse()) return;
        enabled = protectableBlocks.stream().anyMatch(material -> material.createBlockData() instanceof Speleothem);
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
        if (block.getBlockData() instanceof final Speleothem speleothem) {
            final Set<Block> blocks = new HashSet<>();
            final BlockFace verticalDirection = speleothem.getVerticalDirection();
            for (Block next = block.getRelative(verticalDirection); next.getBlockData() instanceof final Speleothem nextSpeleothem && verticalDirection.equals(nextSpeleothem.getVerticalDirection()); next = next.getRelative(verticalDirection)) {
                blocks.add(next);
            }
            return Match.ofBlocks(blocks);
        }
        final Block above = block.getRelative(BlockFace.UP);
        if (above.getBlockData() instanceof final Speleothem speleothem && BlockFace.UP.equals(speleothem.getVerticalDirection())) {
            final Set<Block> blocks = new HashSet<>();
            for (Block next = above; next.getBlockData() instanceof final Speleothem nextSpeleothem && BlockFace.UP.equals(nextSpeleothem.getVerticalDirection()); next = next.getRelative(BlockFace.UP)) {
                blocks.add(next);
            }
            return Match.ofBlocks(blocks);
        }
        final Block below = block.getRelative(BlockFace.DOWN);
        if (below.getBlockData() instanceof final Speleothem speleothem && BlockFace.DOWN.equals(speleothem.getVerticalDirection())) {
            final Set<Block> blocks = new HashSet<>();
            for (Block next = below; next.getBlockData() instanceof final Speleothem nextSpeleothem && BlockFace.DOWN.equals(nextSpeleothem.getVerticalDirection()); next = next.getRelative(BlockFace.DOWN)) {
                blocks.add(next);
            }
            return Match.ofBlocks(blocks);
        }
        return null;
    }
}

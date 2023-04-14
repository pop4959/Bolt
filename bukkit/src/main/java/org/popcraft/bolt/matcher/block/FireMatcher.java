package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Fire;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class FireMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> FIRE_FACES = EnumSet.of(BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> material.createBlockData() instanceof Fire);
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
        final Set<Block> blocks = new HashSet<>();
        for (final BlockFace blockFace : FIRE_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getBlockData() instanceof final Fire fire && ((BlockFace.UP.equals(blockFace) && fire.getFaces().isEmpty()) || fire.getFaces().contains(blockFace.getOppositeFace()))) {
                blocks.add(adjacent);
            }
        }
        return Match.ofBlocks(blocks);
    }
}

package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class VineMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> VINE_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.DOWN);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.contains(Material.VINE);
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
        if (Material.VINE.equals(block.getType())) {
            final Set<Block> blocks = findVines(block);
            return Match.ofBlocks(blocks);
        } else {
            final Set<Block> blocks = new HashSet<>();
            for (final BlockFace blockFace : VINE_FACES) {
                final Block adjacent = block.getRelative(blockFace);
                if (Material.VINE.equals(adjacent.getType())) {
                    blocks.add(adjacent);
                    blocks.addAll(findVines(adjacent));
                }
            }
            return Match.ofBlocks(blocks);
        }
    }

    private Set<Block> findVines(final Block vine) {
        final Set<Block> blocks = new HashSet<>();
        for (Block next = vine.getRelative(BlockFace.DOWN); Material.VINE.equals(next.getType()) && isUnsupported(next); next = next.getRelative(BlockFace.DOWN)) {
            blocks.add(next);
        }
        return blocks;
    }

    private boolean isUnsupported(final Block vine) {
        if (vine.getBlockData() instanceof final MultipleFacing multipleFacing) {
            for (final BlockFace blockFace : multipleFacing.getFaces()) {
                if (vine.getRelative(blockFace).getType().isSolid()) {
                    return false;
                }
            }
        }
        return true;
    }
}

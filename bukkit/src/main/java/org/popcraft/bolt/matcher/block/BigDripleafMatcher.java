package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.HashSet;
import java.util.Set;

public class BigDripleafMatcher implements BlockMatcher {
    private static final Set<Material> BIG_DRIPLEAF_BLOCKS = Set.of(Material.BIG_DRIPLEAF, Material.BIG_DRIPLEAF_STEM);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(BIG_DRIPLEAF_BLOCKS::contains);
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
        if (BIG_DRIPLEAF_BLOCKS.contains(block.getType())) {
            final Set<Block> blocks = new HashSet<>();
            final Block below = block.getRelative(BlockFace.DOWN);
            if (BIG_DRIPLEAF_BLOCKS.contains(below.getType())) {
                for (Block next = block.getRelative(BlockFace.DOWN); BIG_DRIPLEAF_BLOCKS.contains(next.getType()); next = next.getRelative(BlockFace.DOWN)) {
                    blocks.add(next);
                }
            }
            final Block above = block.getRelative(BlockFace.UP);
            if (BIG_DRIPLEAF_BLOCKS.contains(above.getType())) {
                for (Block next = block.getRelative(BlockFace.UP); BIG_DRIPLEAF_BLOCKS.contains(next.getType()); next = next.getRelative(BlockFace.UP)) {
                    blocks.add(next);
                }
            }
            return Match.ofBlocks(blocks);
        } else {
            final Block above = block.getRelative(BlockFace.UP);
            if (BIG_DRIPLEAF_BLOCKS.contains(above.getType())) {
                final Set<Block> blocks = new HashSet<>();
                for (Block next = block.getRelative(BlockFace.UP); BIG_DRIPLEAF_BLOCKS.contains(next.getType()); next = next.getRelative(BlockFace.UP)) {
                    blocks.add(next);
                }
                return Match.ofBlocks(blocks);
            }
        }
        return null;
    }
}

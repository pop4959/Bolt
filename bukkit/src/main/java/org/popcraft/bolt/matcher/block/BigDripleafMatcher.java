package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.popcraft.bolt.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BigDripleafMatcher implements BlockMatcher {
    private static final EnumSet<Material> BIG_DRIPLEAF_BLOCKS = EnumSet.of(Material.BIG_DRIPLEAF, Material.BIG_DRIPLEAF_STEM);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
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
            return Optional.of(Match.ofBlocks(blocks));
        } else {
            final Block above = block.getRelative(BlockFace.UP);
            if (BIG_DRIPLEAF_BLOCKS.contains(above.getType())) {
                final Set<Block> blocks = new HashSet<>();
                for (Block next = block.getRelative(BlockFace.UP); BIG_DRIPLEAF_BLOCKS.contains(next.getType()); next = next.getRelative(BlockFace.UP)) {
                    blocks.add(next);
                }
                return Optional.of(Match.ofBlocks(blocks));
            }
        }
        return Optional.empty();
    }
}

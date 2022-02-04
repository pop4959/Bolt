package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Cocoa;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public class CocoaMatcher implements BlockMatcher {
    private static final EnumSet<Material> GROWTH_BLOCKS = EnumSet.of(Material.JUNGLE_LOG, Material.JUNGLE_WOOD, Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_JUNGLE_WOOD);
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);

    @Override
    public boolean canMatch(Block block) {
        return GROWTH_BLOCKS.contains(block.getType());
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        for (final BlockFace blockFace : CARDINAL_FACES) {
            final Block wall = block.getRelative(blockFace);
            if (wall.getBlockData() instanceof final Cocoa cocoa && blockFace.getOppositeFace().equals(cocoa.getFacing())) {
                return Optional.of(Match.ofBlocks(Collections.singleton(wall)));
            }
        }
        return Optional.empty();
    }
}

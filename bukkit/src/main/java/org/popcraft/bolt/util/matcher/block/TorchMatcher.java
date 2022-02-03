package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public class TorchMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private static final EnumSet<Material> TORCHES = EnumSet.of(Material.TORCH, Material.REDSTONE_TORCH, Material.SOUL_TORCH);
    private static final EnumSet<Material> WALL_TORCHES = EnumSet.of(Material.WALL_TORCH, Material.REDSTONE_WALL_TORCH, Material.SOUL_WALL_TORCH);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        if (TORCHES.contains(above.getType())) {
            return Optional.of(Match.ofBlocks(Collections.singleton(above)));
        } else {
            for (final BlockFace blockFace : CARDINAL_FACES) {
                final Block adjacent = block.getRelative(blockFace);
                if (WALL_TORCHES.contains(adjacent.getType()) && adjacent.getBlockData() instanceof final Directional directional && blockFace.equals(directional.getFacing())) {
                    return Optional.of(Match.ofBlocks(Collections.singleton(adjacent)));
                }
            }
        }
        return Optional.empty();
    }
}

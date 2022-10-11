package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.popcraft.bolt.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ChorusMatcher implements BlockMatcher {
    private static final EnumSet<Material> CHORUS = EnumSet.of(Material.CHORUS_PLANT, Material.CHORUS_FLOWER);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (CHORUS.contains(block.getType())) {
            final Set<Block> blocks = new HashSet<>();
            findChorus(blocks, block);
            return Optional.of(Match.ofBlocks(blocks));
        }
        final Block above = block.getRelative(BlockFace.UP);
        if (CHORUS.contains(above.getType())) {
            final Set<Block> blocks = new HashSet<>();
            findChorus(blocks, above);
            return Optional.of(Match.ofBlocks(blocks));
        }
        return Optional.empty();
    }

    private void findChorus(final Set<Block> blocks, final Block current) {
        if (Material.CHORUS_FLOWER.equals(current.getType())) {
            blocks.add(current);
        } else if (Material.CHORUS_PLANT.equals(current.getType()) && current.getBlockData() instanceof final MultipleFacing multipleFacing) {
            blocks.add(current);
            for (final BlockFace blockFace : multipleFacing.getFaces()) {
                if (!BlockFace.DOWN.equals(blockFace)) {
                    final Block next = current.getRelative(blockFace);
                    if (!blocks.contains(next)) {
                        findChorus(blocks, next);
                    }
                }
            }
        }
    }
}

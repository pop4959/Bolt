package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.popcraft.bolt.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class HangingVineMatcher implements BlockMatcher {
    private static final EnumSet<Material> WEEPING_VINES = EnumSet.of(Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Block below = block.getRelative(BlockFace.DOWN);
        if (Tag.CAVE_VINES.isTagged(block.getType()) || Tag.CAVE_VINES.isTagged(below.getType()) || WEEPING_VINES.contains(block.getType()) || WEEPING_VINES.contains(below.getType())) {
            final Set<Block> blocks = new HashSet<>();
            for (Block next = block.getRelative(BlockFace.DOWN); Tag.CAVE_VINES.isTagged(next.getType()) || WEEPING_VINES.contains(next.getType()); next = next.getRelative(BlockFace.DOWN)) {
                blocks.add(next);
            }
            return Optional.of(Match.ofBlocks(blocks));
        }
        return Optional.empty();
    }
}

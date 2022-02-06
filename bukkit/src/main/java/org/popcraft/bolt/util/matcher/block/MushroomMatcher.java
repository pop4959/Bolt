package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public class MushroomMatcher implements BlockMatcher {
    private static final EnumSet<Material> MUSHROOMS = EnumSet.of(Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.WARPED_FUNGUS, Material.CRIMSON_FUNGUS);

    @Override
    public boolean canMatch(Block block) {
        return true;
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        if (MUSHROOMS.contains(above.getType())) {
            return Optional.of(Match.ofBlocks(Collections.singleton(above)));
        }
        return Optional.empty();
    }
}

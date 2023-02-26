package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class BannerMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(Tag.BANNERS::isTagged);
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
    public Optional<Match> findMatch(Block block) {
        final Block above = block.getRelative(BlockFace.UP);
        if (Tag.BANNERS.isTagged(above.getType()) && !(above.getBlockData() instanceof Directional)) {
            return Optional.of(Match.ofBlocks(Collections.singleton(above)));
        }
        for (final BlockFace blockFace : CARDINAL_FACES) {
            final Block wall = block.getRelative(blockFace);
            if (Tag.BANNERS.isTagged(wall.getType()) && wall.getBlockData() instanceof final Directional directional && blockFace.equals(directional.getFacing())) {
                return Optional.of(Match.ofBlocks(Collections.singleton(wall)));
            }
        }
        return Optional.empty();
    }
}

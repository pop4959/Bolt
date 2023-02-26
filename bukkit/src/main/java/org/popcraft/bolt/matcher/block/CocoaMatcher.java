package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Cocoa;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public class CocoaMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(Tag.JUNGLE_LOGS::isTagged);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean canMatch(Block block) {
        return enabled && Tag.JUNGLE_LOGS.isTagged(block.getType());
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

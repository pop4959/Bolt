package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.CoralWallFan;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class CoralMatcher implements BlockMatcher {
    private static final Set<Material> DEAD_CORALS = Set.of(Material.DEAD_BRAIN_CORAL, Material.DEAD_BUBBLE_CORAL, Material.DEAD_FIRE_CORAL, Material.DEAD_HORN_CORAL, Material.DEAD_TUBE_CORAL);
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> Tag.CORALS.isTagged(material) || DEAD_CORALS.contains(material) || material.createBlockData() instanceof CoralWallFan);
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
        for (final BlockFace blockFace : CARDINAL_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (adjacent.getBlockData() instanceof final CoralWallFan coralWallFan && blockFace.equals(coralWallFan.getFacing())) {
                return Match.ofBlocks(Collections.singleton(adjacent));
            }
        }
        final Block above = block.getRelative(BlockFace.UP);
        if ((Tag.CORALS.isTagged(above.getType()) || DEAD_CORALS.contains(above.getType())) && !(above.getBlockData() instanceof CoralWallFan)) {
            return Match.ofBlocks(Collections.singleton(above));
        }
        return null;
    }
}

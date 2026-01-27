package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class TorchMatcher implements BlockMatcher {
    private static final Material COPPER_TORCH = EnumUtil.valueOf(Material.class, "COPPER_TORCH").orElse(null);
    private static final Material COPPER_WALL_TORCH = EnumUtil.valueOf(Material.class, "COPPER_WALL_TORCH").orElse(null);
    private static final EnumSet<BlockFace> CARDINAL_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);
    // Future: make immutable
    private static final Set<Material> TORCHES = new HashSet<>(Set.of(Material.TORCH, Material.REDSTONE_TORCH, Material.SOUL_TORCH));
    private static final Set<Material> WALL_TORCHES = new HashSet<>(Set.of(Material.WALL_TORCH, Material.REDSTONE_WALL_TORCH, Material.SOUL_WALL_TORCH));
    private boolean enabled;

    static {
        // Future: Replace with Material.COPPER_TORCH
        if (COPPER_TORCH != null) {
            TORCHES.add(COPPER_TORCH);
        }
        // Future: Replace with Material.COPPER_WALL_TORCH
        if (COPPER_WALL_TORCH != null) {
            WALL_TORCHES.add(COPPER_WALL_TORCH);
        }
    }

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> TORCHES.contains(material) || WALL_TORCHES.contains(material));
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
        final Block above = block.getRelative(BlockFace.UP);
        if (TORCHES.contains(above.getType())) {
            return Match.ofBlocks(Collections.singleton(above));
        } else {
            for (final BlockFace blockFace : CARDINAL_FACES) {
                final Block adjacent = block.getRelative(blockFace);
                if (WALL_TORCHES.contains(adjacent.getType()) && adjacent.getBlockData() instanceof final Directional directional && blockFace.equals(directional.getFacing())) {
                    return Match.ofBlocks(Collections.singleton(adjacent));
                }
            }
        }
        return null;
    }
}

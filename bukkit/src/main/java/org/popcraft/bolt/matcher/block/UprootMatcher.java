package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class UprootMatcher implements BlockMatcher {
    private static final EnumSet<Material> UPROOT = EnumSet.of(Material.BAMBOO, Material.CACTUS, Material.SUGAR_CANE, Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT, Material.KELP, Material.KELP_PLANT);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(UPROOT::contains);
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
        if (UPROOT.contains(block.getType()) || UPROOT.contains(above.getType())) {
            final Set<Block> blocks = new HashSet<>();
            for (Block next = block.getRelative(BlockFace.UP); UPROOT.contains(next.getType()); next = next.getRelative(BlockFace.UP)) {
                blocks.add(next);
            }
            return Optional.of(Match.ofBlocks(blocks));
        }
        return Optional.empty();
    }
}

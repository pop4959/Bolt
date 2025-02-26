package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.HashSet;
import java.util.Set;

public class HangingVineMatcher implements BlockMatcher {
    private static final Set<Material> WEEPING_VINES = Set.of(Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT);
    private static final Material PALE_HANGING_MOSS = EnumUtil.valueOf(Material.class, "PALE_HANGING_MOSS").orElse(null);
    private boolean enabled;

    private boolean isHangingVine(Material material) {
        if (Tag.CAVE_VINES.isTagged(material) || WEEPING_VINES.contains(material)) {
            return true;
        }
        // Future: Replace with Material.PALE_HANGING_MOSS and put into the set with weeping vines
        if (PALE_HANGING_MOSS == null) {
            return false;
        } else {
            return PALE_HANGING_MOSS.equals(material);
        }
    }

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(this::isHangingVine);
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
        final Block below = block.getRelative(BlockFace.DOWN);
        if (isHangingVine(block.getType()) || isHangingVine(below.getType())) {
            final Set<Block> blocks = new HashSet<>();
            for (Block next = block.getRelative(BlockFace.DOWN); isHangingVine(next.getType()); next = next.getRelative(BlockFace.DOWN)) {
                blocks.add(next);
            }
            return Match.ofBlocks(blocks);
        }
        return null;
    }
}

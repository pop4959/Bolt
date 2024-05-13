package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.HashSet;
import java.util.Set;

public class HangingSignMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(Tag.CEILING_HANGING_SIGNS::isTagged);
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
        final Set<Block> blocks = new HashSet<>();
        Block below = block;
        while (Tag.CEILING_HANGING_SIGNS.isTagged((below = below.getRelative(BlockFace.DOWN)).getType())) {
            blocks.add(below);
        }
        return Match.ofBlocks(blocks);
    }
}

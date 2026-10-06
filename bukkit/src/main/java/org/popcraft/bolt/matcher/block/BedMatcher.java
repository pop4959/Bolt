package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.Set;

public class BedMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> material.createBlockData() instanceof Bed);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean canMatch(Block block) {
        return enabled && block.getBlockData() instanceof Bed;
    }

    @Override
    public Match findMatch(Block block) {
        if (block.getBlockData() instanceof final Bed bed) {
            if (Bed.Part.FOOT.equals(bed.getPart())) {
                final Block head = block.getRelative(bed.getFacing());
                if (head.getBlockData() instanceof Bed) {
                    return Match.ofBlocks(Collections.singleton(head));
                }
            } else {
                final Block foot = block.getRelative(bed.getFacing().getOppositeFace());
                if (foot.getBlockData() instanceof Bed) {
                    return Match.ofBlocks(Collections.singleton(foot));
                }
            }
        }
        return null;
    }
}

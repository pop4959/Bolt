package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TallFlowerMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(Tag.TALL_FLOWERS::isTagged);
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
        if (Tag.TALL_FLOWERS.isTagged(block.getType()) && block.getBlockData() instanceof final Bisected bisected) {
            final Bisected.Half half = bisected.getHalf();
            if (Bisected.Half.BOTTOM.equals(half)) {
                return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(BlockFace.UP))));
            } else {
                return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(BlockFace.DOWN))));
            }
        } else {
            final Block above = block.getRelative(BlockFace.UP);
            if (Tag.TALL_FLOWERS.isTagged(above.getType())) {
                return Optional.of(Match.ofBlocks(List.of(above, above.getRelative(BlockFace.UP))));
            }
        }
        return Optional.empty();
    }
}

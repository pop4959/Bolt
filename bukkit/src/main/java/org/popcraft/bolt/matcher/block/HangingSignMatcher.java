package org.popcraft.bolt.matcher.block;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class HangingSignMatcher implements BlockMatcher {
    private static final Tag<Material> CEILING_HANGING_SIGNS = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft("ceiling_hanging_signs"), Material.class);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        // Future: Replace with Tag.CEILING_HANGING_SIGNS
        if (CEILING_HANGING_SIGNS == null) {
            enabled = false;
        } else {
            enabled = protectableBlocks.stream().anyMatch(CEILING_HANGING_SIGNS::isTagged);
        }
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
        if (CEILING_HANGING_SIGNS == null) {
            return Optional.empty();
        }
        final Set<Block> blocks = new HashSet<>();
        Block below = block;
        while (CEILING_HANGING_SIGNS.isTagged((below = below.getRelative(BlockFace.DOWN)).getType())) {
            blocks.add(below);
        }
        return Optional.of(Match.ofBlocks(blocks));
    }
}

package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;
import org.popcraft.bolt.util.FoliaUtil;

import java.util.HashSet;
import java.util.Set;

public class CushionMatcher implements BlockMatcher {
    private static final EntityType CUSHION = EnumUtil.valueOf(EntityType.class, "CUSHION").orElse(null);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableEntities.stream().anyMatch(entityType -> entityType.equals(CUSHION));
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
        final Set<Entity> entities = new HashSet<>();
        FoliaUtil.getNearbyEntities(block, block.getBoundingBox().expand(0, 0, 0, 0, 0.5, 0), (entity -> entity.getType().equals(CUSHION))).forEach(entity -> {
            // Cushions may be "in" blocks (i.e. on a slab)
            if (entity.getType().equals(CUSHION) && (
                    entity.getLocation().toBlockLocation().equals(block.getLocation()) ||
                    entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation().equals(block.getLocation())
            )) {
                entities.add(entity);
            }
        });
        return Match.ofEntities(entities);
    }
}

package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.EnumUtil;

import java.util.Set;

public class CushionMatcher implements BlockMatcher {
    // Future: Replace with EntityType.CUSHION
    private static final EntityType CUSHION = EnumUtil.valueOf(EntityType.class, "CUSHION").orElse(null);
    private boolean enabled;
    private BoltPlugin plugin;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableEntities.stream().anyMatch(entityType -> entityType.equals(CUSHION));
        this.plugin = JavaPlugin.getPlugin(BoltPlugin.class);
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
        return Match.ofEntities(this.plugin.getHangingCache().entitiesSupportedByBlock(block));
    }
}

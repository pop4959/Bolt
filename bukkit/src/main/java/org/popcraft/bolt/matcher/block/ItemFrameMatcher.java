package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.util.FoliaUtil;
import org.popcraft.bolt.util.HangingCache;

import java.util.HashSet;
import java.util.Set;

public class ItemFrameMatcher implements BlockMatcher {
    private boolean enabled;
    private BoltPlugin plugin;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableEntities.stream().anyMatch(entityType -> EntityType.ITEM_FRAME.equals(entityType) || EntityType.GLOW_ITEM_FRAME.equals(entityType));
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

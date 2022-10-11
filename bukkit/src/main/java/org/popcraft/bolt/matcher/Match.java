package org.popcraft.bolt.matcher;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;

public record Match(Collection<Block> blocks, Collection<Entity> entities) {
    public static Match of(Collection<Block> blocks, Collection<Entity> entities) {
        return new Match(blocks, entities);
    }

    public static Match ofBlocks(Collection<Block> blocks) {
        return new Match(blocks, Collections.emptySet());
    }

    public static Match ofEntities(Collection<Entity> entities) {
        return new Match(Collections.emptySet(), entities);
    }

    public static Match empty() {
        return new Match(Collections.emptySet(), Collections.emptySet());
    }
}

package org.popcraft.bolt.matcher;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Set;

public interface Matcher<T> {
    void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities);

    boolean enabled();

    boolean canMatch(T type);

    Match findMatch(T type);
}

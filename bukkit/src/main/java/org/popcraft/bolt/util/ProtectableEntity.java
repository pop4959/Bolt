package org.popcraft.bolt.util;

import org.bukkit.entity.Entity;
import org.popcraft.bolt.protection.EntityProtection;

import java.util.HashMap;
import java.util.UUID;

public record ProtectableEntity(Entity entity) implements Protectable {
    @Override
    public String getTypeName() {
        return entity.getType().name();
    }

    @Override
    public EntityProtection createProtection(final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new EntityProtection(entity.getUniqueId(), owner, type, now, now, new HashMap<>(), entity.getType().name());
    }
}

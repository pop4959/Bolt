package org.popcraft.bolt.protection;

import org.popcraft.bolt.util.Source;

import java.util.Map;
import java.util.UUID;

public class EntityProtection extends Protection {
    private final String entity;

    public EntityProtection(UUID id, UUID owner, String type, Map<Source, String> access, String entity) {
        super(id, owner, type, access);
        this.entity = entity;
    }

    public String getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return "EntityProtection{" +
                "id=" + id +
                ", owner=" + owner +
                ", type='" + type + '\'' +
                ", access=" + access +
                ", entity='" + entity + '\'' +
                '}';
    }
}

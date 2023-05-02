package org.popcraft.bolt.protection;

import java.util.Map;
import java.util.UUID;

public class EntityProtection extends Protection {
    private String entity;

    public EntityProtection(UUID id, UUID owner, String type, long created, long accessed, Map<String, String> access, String entity) {
        super(id, owner, type, created, accessed, access);
        this.entity = entity;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return "EntityProtection{" +
                "id=" + id +
                ", owner=" + owner +
                ", type='" + type + '\'' +
                ", created=" + created +
                ", accessed=" + accessed +
                ", access=" + access +
                ", entity='" + entity + '\'' +
                '}';
    }
}

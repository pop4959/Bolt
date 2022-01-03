package org.popcraft.bolt.data.protection;

import org.popcraft.bolt.data.Source;

import java.util.Map;
import java.util.UUID;

public abstract class Protection {
    private final UUID id;
    private final Map<Source, String> accessList;
    private UUID owner;
    private String type;

    protected Protection(UUID id, UUID owner, String type, Map<Source, String> accessList) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.accessList = accessList;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<Source, String> getAccessList() {
        return accessList;
    }
}

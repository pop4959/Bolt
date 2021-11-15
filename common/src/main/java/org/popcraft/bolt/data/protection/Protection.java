package org.popcraft.bolt.data.protection;

import org.popcraft.bolt.data.Source;

import java.util.Map;
import java.util.UUID;

public abstract class Protection {
    private final UUID id;
    private final Map<Source, String> accessList;
    private String owner;
    private String type;

    protected Protection(UUID id, String owner, String type, Map<Source, String> accessList) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.accessList = accessList;
    }

    public boolean canAccess(final UUID player) {
        // TODO: Check permissions
        // TODO: Think about how to handle protection type access and access list
        return player.toString().equals(owner);
    }

    public UUID getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
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

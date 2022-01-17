package org.popcraft.bolt.data.protection;

import org.popcraft.bolt.data.Source;

import java.util.Map;
import java.util.UUID;

public abstract class Protection {
    protected final UUID id;
    protected final Map<Source, String> accessList;
    protected UUID owner;
    protected String type;

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

    @Override
    public String toString() {
        return "Protection{" +
                "id=" + id +
                ", accessList=" + accessList +
                ", owner=" + owner +
                ", type='" + type + '\'' +
                '}';
    }
}

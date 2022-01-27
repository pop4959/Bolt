package org.popcraft.bolt.protection;

import org.popcraft.bolt.util.Source;

import java.util.Map;
import java.util.UUID;

public abstract class Protection {
    protected final UUID id;
    protected final Map<Source, String> access;
    protected UUID owner;
    protected String type;

    protected Protection(UUID id, UUID owner, String type, Map<Source, String> access) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.access = access;
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

    public Map<Source, String> getAccess() {
        return access;
    }

    @Override
    public String toString() {
        return "Protection{" +
                "id=" + id +
                ", owner=" + owner +
                ", type='" + type + '\'' +
                ", access=" + access +
                '}';
    }
}

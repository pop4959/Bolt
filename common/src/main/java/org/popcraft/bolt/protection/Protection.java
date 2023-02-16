package org.popcraft.bolt.protection;

import java.util.Map;
import java.util.UUID;

public abstract class Protection {
    protected final UUID id;
    protected UUID owner;
    protected String type;
    protected long created;
    protected long accessed;
    protected Map<String, String> access;

    protected Protection(UUID id, UUID owner, String type, long created, long accessed, Map<String, String> access) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.created = created;
        this.accessed = accessed;
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

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getAccessed() {
        return accessed;
    }

    public void setAccessed(long accessed) {
        this.accessed = accessed;
    }

    public Map<String, String> getAccess() {
        return access;
    }

    public void setAccess(Map<String, String> access) {
        this.access = access;
    }

    @Override
    public String toString() {
        return "Protection{" +
                "id=" + id +
                ", owner=" + owner +
                ", type='" + type + '\'' +
                ", created=" + created +
                ", accessed=" + accessed +
                ", access=" + access +
                '}';
    }
}

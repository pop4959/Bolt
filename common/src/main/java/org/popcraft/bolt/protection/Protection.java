package org.popcraft.bolt.protection;

import java.util.Map;
import java.util.UUID;

public abstract class Protection {
    protected final UUID id;
    protected final UUID owner;
    protected final String type;
    protected final long created;
    protected final long accessed;
    protected final Map<String, String> access;

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

    public String getType() {
        return type;
    }

    public long getCreated() {
        return created;
    }

    public long getAccessed() {
        return accessed;
    }

    public Map<String, String> getAccess() {
        return access;
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

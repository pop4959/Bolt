package org.popcraft.bolt.data.protection;

import org.popcraft.bolt.data.Source;

import java.util.Map;
import java.util.UUID;

public abstract class Protection {
    private final UUID id;
    private final Map<Source, String> access;
    private String owner;
    private String type;

    public Protection(UUID id, String owner, String type, Map<Source, String> access) {
        this.id = id;
        this.owner = owner;
        this.type = type;
        this.access = access;
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

    public Map<Source, String> getAccess() {
        return access;
    }
}

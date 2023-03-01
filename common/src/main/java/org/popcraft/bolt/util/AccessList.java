package org.popcraft.bolt.util;

import java.util.Map;
import java.util.UUID;

public class AccessList {
    private final UUID owner;
    private final Map<String, String> access;

    public AccessList(UUID owner, Map<String, String> access) {
        this.owner = owner;
        this.access = access;
    }

    public UUID getOwner() {
        return owner;
    }

    public Map<String, String> getAccess() {
        return access;
    }

    @Override
    public String toString() {
        return "AccessList{" +
                "owner=" + owner +
                ", access=" + access +
                '}';
    }
}

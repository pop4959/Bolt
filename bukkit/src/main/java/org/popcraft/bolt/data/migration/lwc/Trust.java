package org.popcraft.bolt.data.migration.lwc;

import java.util.List;
import java.util.UUID;

public class Trust {
    private UUID owner;
    private List<UUID> trusted;

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public List<UUID> getTrusted() {
        return trusted;
    }

    public void setTrusted(List<UUID> trusted) {
        this.trusted = trusted;
    }
}

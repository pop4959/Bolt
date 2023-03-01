package org.popcraft.bolt.util;

import java.util.List;
import java.util.UUID;

public class Group {
    private final String name;
    private final UUID owner;
    private final List<UUID> members;

    public Group(String name, UUID owner, List<UUID> members) {
        this.name = name;
        this.owner = owner;
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<UUID> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return "Group{" +
                "name='" + name + '\'' +
                ", owner=" + owner +
                ", members=" + members +
                '}';
    }
}

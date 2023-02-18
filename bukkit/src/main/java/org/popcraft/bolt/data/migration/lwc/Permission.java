package org.popcraft.bolt.data.migration.lwc;

@SuppressWarnings("unused")
public class Permission {
    public enum Access {
        NONE,
        PLAYER,
        ADMIN
    }

    public enum Type {
        GROUP,
        PLAYER,
        RESERVED,
        TOWN,
        ITEM,
        REGION,
        FACTION
    }
}

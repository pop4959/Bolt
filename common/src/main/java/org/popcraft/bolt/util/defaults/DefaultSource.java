package org.popcraft.bolt.util.defaults;

public enum DefaultSource {
    PLAYER("player"),
    GROUP("group"),
    PERMISSION("permission"),
    PASSWORD("password"),
    REDSTONE("redstone"),
    HOPPER("hopper");

    private final String type;

    private DefaultSource(final String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}

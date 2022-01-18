package org.popcraft.bolt.util.defaults;

public enum DefaultPermission {
    BREAK("break"),
    PLACE("place"),
    CREATE("create"),
    KILL("kill"),
    CONTAINER_ACCESS("container_access"),
    CONTAINER_ADD("container_add"),
    CONTAINER_REMOVE("container_remove"),
    INTERACT("interact"),
    MODIFY("modify");

    private final String key;

    private DefaultPermission(final String key) {
        this.key = key;
    }

    public java.lang.String getKey() {
        return key;
    }
}

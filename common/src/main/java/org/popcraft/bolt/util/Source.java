package org.popcraft.bolt.util;

import java.util.UUID;

public final class Source {
    private Source() {
    }

    public static final String PLAYER = "player";
    public static final String GROUP = "group";
    public static final String PERMISSION = "permission";
    public static final String PASSWORD = "password";
    public static final String REDSTONE = "redstone";
    public static final String BLOCK = "block";
    public static final String TOWN = "town";
    public static final String REGION = "region";

    public static String fromPlayer(final UUID uuid) {
        return "player:" + uuid.toString();
    }

    public static String from(final String type, final String identifier) {
        return type + ':' + identifier;
    }

    public static String type(final String source) {
        return source.split(":")[0];
    }

    public static String identifier(final String source) {
        return source.split(":")[1];
    }
}

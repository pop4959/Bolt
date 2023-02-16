package org.popcraft.bolt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static String fromPassword(final String password) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(password.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hash = new StringBuilder();
            for (final byte b : messageDigest.digest()) {
                hash.append("%02x".formatted(b));
            }
            return Source.from(Source.PASSWORD, hash.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return Source.from(Source.PASSWORD, Source.PASSWORD);
        }
    }

    public static String from(final String type, final String identifier) {
        return type + ':' + identifier;
    }

    public static String from(final String type) {
        return "%s:%s".formatted(type, type);
    }

    public static String type(final String source) {
        return source.split(":")[0];
    }

    public static String identifier(final String source) {
        return source.split(":")[1];
    }
}

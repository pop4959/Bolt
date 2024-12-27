package org.popcraft.bolt.source;

public final class SourceTypes {
    private SourceTypes() {
    }

    /**
     * Represents a player. The identifier is the UUID of the player
     */
    public static final String PLAYER = "player";
    /**
     * Represents anyone who knows a password. The identifier is the SHA-1 hash of the actual password.
     */
    public static final String PASSWORD = "password";
    /**
     * Represents anyone with the permission node in the identifier.
     */
    public static final String PERMISSION = "permission";
    /**
     * Represents anyone part of a Bolt group ({@code /bolt group}).
     */
    public static final String GROUP = "group";
    /**
     * Represents a redstone signal. Identifier does not matter.
     */
    public static final String REDSTONE = "redstone";
    /**
     * Represents a block. Identifier does not matter.
     */
    public static final String BLOCK = "block";
    /**
     * Represents a door. Used for autoclose. Identifier does not matter.
     */
    public static final String DOOR = "door";
    public static final String REGION = "region";
    public static final String TOWN = "town";
    public static final String FACTION = "faction";
    public static final String ENTITY = "entity";
}

package org.popcraft.bolt.compat;

public class EntityTypeMapper {
    public static String map(final String entityType) {
        return switch (entityType) {
            case "DROPPED_ITEM" -> "ITEM";
            case "LEASH_HITCH" -> "LEASH_KNOT";
            case "ENDER_SIGNAL" -> "EYE_OF_ENDER";
            case "SPLASH_POTION" -> "POTION";
            case "THROWN_EXP_BOTTLE" -> "EXPERIENCE_BOTTLE";
            case "PRIMED_TNT" -> "TNT";
            case "FIREWORK" -> "FIREWORK_ROCKET";
            case "MINECART_COMMAND" -> "COMMAND_BLOCK_MINECART";
            case "MINECART_CHEST" -> "CHEST_MINECART";
            case "MINECART_FURNACE" -> "FURNACE_MINECART";
            case "MINECART_TNT" -> "TNT_MINECART";
            case "MINECART_HOPPER" -> "HOPPER_MINECART";
            case "MINECART_MOB_SPAWNER" -> "SPAWNER_MINECART";
            case "MUSHROOM_COW" -> "MOOSHROOM";
            case "SNOWMAN" -> "SNOW_GOLEM";
            case "ENDER_CRYSTAL" -> "END_CRYSTAL";
            case "FISHING_HOOK" -> "FISHING_BOBBER";
            case "LIGHTNING" -> "LIGHTNING_BOLT";
            default -> entityType;
        };
    }
}

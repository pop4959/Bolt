package org.popcraft.bolt.util;

import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.popcraft.bolt.lang.Strings;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.popcraft.bolt.lang.Translator.translate;

public final class Protections {
    // Future: Remove when support for lower than 1.19.3 is dropped
    private static boolean translatableSupport;

    static {
        try {
            Class.forName("org.bukkit.Translatable");
            translatableSupport = true;
        } catch (ClassNotFoundException e) {
            translatableSupport = false;
        }
    }

    private Protections() {
    }

    public static String displayType(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            final World world = Bukkit.getWorld(blockProtection.getWorld());
            final int x = blockProtection.getX();
            final int y = blockProtection.getY();
            final int z = blockProtection.getZ();
            if (world == null || !world.isChunkLoaded(x >> 4, z >> 4)) {
                return Strings.toTitleCase(blockProtection.getBlock());
            } else {
                final Block block = world.getBlockAt(x, y, z);
                if (translatableSupport) {
                    return displayType(block);
                } else {
                    return displayTypeFromData(block);
                }
            }
        } else if (protection instanceof final EntityProtection entityProtection) {
            final Entity entity = Bukkit.getServer().getEntity(entityProtection.getId());
            if (entity == null) {
                return Strings.toTitleCase(entityProtection.getEntity());
            } else {
                if (translatableSupport) {
                    return displayType(entity);
                } else {
                    return displayTypeFromData(entity);
                }
            }
        } else {
            return translate(Translation.UNKNOWN);
        }
    }

    public static String displayType(final Block block) {
        final Component translatable = Component.translatable(block.getTranslationKey());
        return BukkitComponentSerializer.legacy().serialize(translatable);
    }

    public static String displayType(final Entity entity) {
        final Component translatable = Component.translatable(entity.getType().getTranslationKey());
        return BukkitComponentSerializer.legacy().serialize(translatable);
    }

    public static String displayTypeFromData(final Block block) {
        final String blockDataString = block.getBlockData().getAsString().replace(':', '.');
        final int nbtIndex = blockDataString.indexOf('[');
        final String key;
        if (nbtIndex > -1) {
            key = blockDataString.substring(0, nbtIndex);
        } else {
            key = blockDataString;
        }
        final Component translatable = Component.translatable("block.%s".formatted(key));
        return BukkitComponentSerializer.legacy().serialize(translatable);
    }

    public static String displayTypeFromData(final Entity entity) {
        return entity.getName();
    }

    public static String accessList(final Map<String, String> accessMap) {
        if (accessMap == null || accessMap.isEmpty()) {
            return "";
        }
        final List<String> lines = new ArrayList<>();
        accessMap.forEach((entry, access) -> {
            final Source source = Source.parse(entry);
            final String subject;
            if (SourceTypes.PLAYER.equals(source.getType())) {
                final String playerUuid = source.getIdentifier();
                final UUID uuid = UUID.fromString(playerUuid);
                final String playerName = BukkitAdapter.findProfileByUniqueId(uuid).name();
                subject = Optional.ofNullable(playerName).orElse(playerUuid);
            } else if (SourceTypes.PASSWORD.equals(source.getType()) || source.getType().equals(source.getIdentifier())) {
                subject = Strings.toTitleCase(source.getType());
            } else {
                subject = source.getIdentifier();
            }
            lines.add("%s (%s: %s)".formatted(subject, Strings.toTitleCase(source.getType()), Strings.toTitleCase(access)));
        });
        return String.join("\n", lines);
    }
}

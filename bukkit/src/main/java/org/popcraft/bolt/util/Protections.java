package org.popcraft.bolt.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
import java.util.Objects;
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

    public static String protectionType(final Protection protection) {
        final String protectionType = protection.getType();
        final String translationKey = "protection_type_%s".formatted(protectionType);
        final String translation = translate(translationKey);
        if (!translationKey.equals(translation)) {
            return translation;
        }
        return Strings.toTitleCase(protectionType);
    }

    public static Component displayType(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            final World world = Bukkit.getWorld(blockProtection.getWorld());
            final int x = blockProtection.getX();
            final int y = blockProtection.getY();
            final int z = blockProtection.getZ();
            if (world == null || !world.isChunkLoaded(x >> 4, z >> 4)) {
                return displayType(Objects.requireNonNullElse(Material.getMaterial(blockProtection.getBlock().toUpperCase()), Material.AIR));
            } else {
                return displayType(world.getBlockAt(x, y, z));
            }
        } else if (protection instanceof final EntityProtection entityProtection) {
            final Entity entity = Bukkit.getServer().getEntity(entityProtection.getId());
            if (entity == null) {
                try {
                    return displayType(EntityType.valueOf(entityProtection.getEntity().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return displayType(EntityType.PIG);
                }
            } else {
                return displayType(entity);
            }
        } else {
            return Component.text(translate(Translation.UNKNOWN));
        }
    }

    public static Component displayType(final Block block) {
        final Material material = block.getType();
        if (translatableSupport) {
            return displayType(material);
        } else {
            return displayTypeFromData(material);
        }
    }

    private static Component displayType(final Material material) {
        final String materialNameLower = material.name().toLowerCase();
        final String blockTranslationKey = "block_%s".formatted(materialNameLower);
        final String blockTranslation = translate(blockTranslationKey);
        if (!blockTranslationKey.equals(blockTranslation)) {
            return Component.text(blockTranslation);
        }
        if (translatableSupport) {
            return Component.translatable(material.getTranslationKey());
        } else {
            return Component.text(Strings.toTitleCase(materialNameLower));
        }
    }

    private static Component displayTypeFromData(final Material material) {
        final String blockDataString = material.createBlockData().getAsString().replace(':', '.');
        final int nbtIndex = blockDataString.indexOf('[');
        final String key;
        if (nbtIndex > -1) {
            key = blockDataString.substring(0, nbtIndex);
        } else {
            key = blockDataString;
        }
        return Component.translatable("block.%s".formatted(key));
    }

    public static Component displayType(final Entity entity) {
        if (translatableSupport) {
            return displayType(entity.getType());
        } else {
            return Component.text(entity.getName());
        }
    }

    private static Component displayType(final EntityType entityType) {
        final String entityTypeLower = entityType.name().toLowerCase();
        final String entityTranslationKey = "entity_%s".formatted(entityTypeLower);
        final String entityTranslation = translate(entityTranslationKey);
        if (!entityTranslationKey.equals(entityTranslation)) {
            return Component.text(entityTranslation);
        }
        if (translatableSupport) {
            return Component.translatable(entityType.getTranslationKey());
        } else {
            return Component.text(Strings.toTitleCase(entityTypeLower));
        }
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

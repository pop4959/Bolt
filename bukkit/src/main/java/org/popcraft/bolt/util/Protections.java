package org.popcraft.bolt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.compat.EntityTypeMapper;
import org.popcraft.bolt.lang.Strings;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.popcraft.bolt.lang.Translator.isTranslatable;
import static org.popcraft.bolt.util.BoltComponents.getLocaleOf;
import static org.popcraft.bolt.util.BoltComponents.resolveTranslation;
import static org.popcraft.bolt.util.BoltComponents.translateRaw;

public final class Protections {

    private Protections() {
    }

    public static Component raw(final Protection protection) {
        return Component.text(protection.toString());
    }

    public static Component protectionType(final Protection protection, final CommandSender sender) {
        final String protectionType = protection.getType();
        final String translationKey = "protection_type_%s".formatted(protectionType);
        if (!isTranslatable(translationKey, getLocaleOf(sender))) {
            return Component.text(Strings.toTitleCase(protectionType));
        }
        return BoltComponents.resolveTranslation(translationKey, sender);
    }

    public static Component sourceType(final Source source, final CommandSender sender) {
        final String sourceType = source.getType();
        final String translationKey = "source_type_%s".formatted(sourceType);
        if (!isTranslatable(translationKey, getLocaleOf(sender))) {
            return Component.text(Strings.toTitleCase(sourceType));
        }
        return BoltComponents.resolveTranslation(translationKey, sender);
    }

    public static Component accessType(final String accessType, final CommandSender sender) {
        final String translationKey = "access_type_%s".formatted(accessType);
        if (!isTranslatable(translationKey, getLocaleOf(sender))) {
            return Component.text(Strings.toTitleCase(accessType));
        }
        return BoltComponents.resolveTranslation(translationKey, sender);
    }

    public static Component displayType(final Protection protection, final CommandSender sender) {
        if (protection instanceof final BlockProtection blockProtection) {
            final World world = Bukkit.getWorld(blockProtection.getWorld());
            final int x = blockProtection.getX();
            final int y = blockProtection.getY();
            final int z = blockProtection.getZ();
            if (world == null || !world.isChunkLoaded(x >> 4, z >> 4)) {
                return displayType(Objects.requireNonNullElse(Material.getMaterial(blockProtection.getBlock().toUpperCase()), Material.AIR), sender);
            } else {
                return displayType(world.getBlockAt(x, y, z), sender);
            }
        } else if (protection instanceof final EntityProtection entityProtection) {
            final Entity entity = Bukkit.getServer().getEntity(entityProtection.getId());
            if (entity == null) {
                try {
                    final String entityType = entityProtection.getEntity().toUpperCase();
                    return displayType(EntityType.valueOf(EntityTypeMapper.map(entityType)), sender);
                } catch (IllegalArgumentException e) {
                    return resolveTranslation(Translation.UNKNOWN, sender);
                }
            } else {
                return displayType(entity, sender);
            }
        } else {
            return resolveTranslation(Translation.UNKNOWN, sender);
        }
    }

    public static Component displayType(final Block block, final CommandSender sender) {
        final Material material = block.getType();
        return displayType(material, sender);
    }

    private static Component displayType(final Material material, final CommandSender sender) {
        final String materialNameLower = material.name().toLowerCase();
        final String blockTranslationKey = "block_%s".formatted(materialNameLower);
        final String blockTranslation = translateRaw(blockTranslationKey, sender);
        if (!blockTranslationKey.equals(blockTranslation)) {
            return Component.text(blockTranslation);
        }
        return Component.translatable(material);
    }

    public static Component displayType(final Entity entity, final CommandSender sender) {
        return displayType(entity.getType(), sender);
    }

    private static Component displayType(final EntityType entityType, final CommandSender sender) {
        final String entityTypeLower = entityType.name().toLowerCase();
        final String entityTranslationKey = "entity_%s".formatted(entityTypeLower);
        final String entityTranslation = translateRaw(entityTranslationKey, sender);
        if (!entityTranslationKey.equals(entityTranslation)) {
            return Component.text(entityTranslation);
        }
        return Component.translatable(entityType);
    }

    public static Component accessList(final Map<String, String> accessMap, final CommandSender sender) {
        if (accessMap == null || accessMap.isEmpty()) {
            return Component.empty();
        }
        final List<Component> list = new ArrayList<>();
        for (final Map.Entry<String, String> accessEntry : accessMap.entrySet()) {
            final String entry = accessEntry.getKey();
            final String access = accessEntry.getValue();
            final Source source = Source.parse(entry);
            final BoltPlugin plugin = JavaPlugin.getPlugin(BoltPlugin.class);
            final String subject;
            if (source.getType().equals(source.getIdentifier())) {
                subject = Strings.toTitleCase(source.getType());
            } else {
                subject = plugin.getSourceTransformer(source.getType()).unTransformIdentifier(source.getIdentifier());
            }
            if (plugin.getDefaultAccessType().equals(access)) {
                list.add(resolveTranslation(
                        Translation.ACCESS_LIST_ENTRY_DEFAULT,
                        sender,
                        Placeholder.component(Translation.Placeholder.SOURCE_IDENTIFIER, Component.text(subject)),
                        Placeholder.component(Translation.Placeholder.SOURCE_TYPE, sourceType(source, sender)),
                        Placeholder.component(Translation.Placeholder.ACCESS_TYPE, accessType(access, sender))
                ));
            } else {
                list.add(resolveTranslation(
                        Translation.ACCESS_LIST_ENTRY,
                        sender,
                        Placeholder.component(Translation.Placeholder.SOURCE_IDENTIFIER, Component.text(subject)),
                        Placeholder.component(Translation.Placeholder.SOURCE_TYPE, sourceType(source, sender)),
                        Placeholder.component(Translation.Placeholder.ACCESS_TYPE, accessType(access, sender))
                ));
            }
        }
        return Component.join(JoinConfiguration.newlines(), list);
    }
}

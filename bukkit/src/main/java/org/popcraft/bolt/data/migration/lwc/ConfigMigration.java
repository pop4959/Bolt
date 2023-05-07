package org.popcraft.bolt.data.migration.lwc;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.AccessRegistry;
import org.popcraft.bolt.access.DefaultAccess;
import org.popcraft.bolt.util.Permission;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ConfigMigration {
    private final BoltPlugin plugin;
    private final String defaultProtectionPrivate;
    private final String defaultProtectionDisplay;
    private final String defaultProtectionDeposit;
    private final String defaultProtectionWithdrawal;
    private final String defaultProtectionPublic;

    public ConfigMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
        final AccessRegistry accessRegistry = plugin.getBolt().getAccessRegistry();
        defaultProtectionPrivate = accessRegistry.findProtectionTypeWithExactPermissions(DefaultAccess.PRIVATE).orElse("private");
        defaultProtectionDisplay = accessRegistry.findProtectionTypeWithExactPermissions(DefaultAccess.DISPLAY).orElse("display");
        defaultProtectionDeposit = accessRegistry.findProtectionTypeWithExactPermissions(DefaultAccess.DEPOSIT).orElse("deposit");
        defaultProtectionWithdrawal = accessRegistry.findProtectionTypeWithExactPermissions(DefaultAccess.WITHDRAWAL).orElse("withdrawal");
        defaultProtectionPublic = accessRegistry.findProtectionTypeWithExactPermissions(DefaultAccess.PUBLIC).orElse("public");
    }

    public void convert() {
        if (!plugin.loadProtections().isEmpty()) {
            return;
        }
        convertCore();
        convertDoors();
    }

    private void convertCore() {
        final FileConfiguration lwcCoreConfig = YamlConfiguration.loadConfiguration(plugin.getPluginsPath().resolve("LWC/core.yml").toFile());
        final ConfigurationSection blocks = lwcCoreConfig.getConfigurationSection("protections.blocks");
        if (blocks == null) {
            return;
        }
        final Map<Material, String> migrateToBoltConfig = new EnumMap<>(Material.class);
        for (final String block : blocks.getKeys(false)) {
            final boolean enabled = blocks.getBoolean("%s.enabled".formatted(block), false);
            final Material material = Material.getMaterial(block.toUpperCase());
            if (material != null && material.isBlock()) {
                migrateToBoltConfig.put(material, enabled ? blocks.getString("%s.autoRegister".formatted(block), "false") : "false");
            }
        }
        if (!migrateToBoltConfig.isEmpty()) {
            migrateToBoltConfig.forEach((material, protectionType) -> {
                final String boltProtectionType;
                if ("private".equals(protectionType)) {
                    boltProtectionType = defaultProtectionPrivate;
                } else if ("display".equals(protectionType)) {
                    boltProtectionType = defaultProtectionDisplay;
                } else if ("donation".equals(protectionType)) {
                    boltProtectionType = defaultProtectionDeposit;
                } else if ("supply".equals(protectionType)) {
                    boltProtectionType = defaultProtectionWithdrawal;
                } else if ("public".equals(protectionType)) {
                    boltProtectionType = defaultProtectionPublic;
                } else {
                    boltProtectionType = protectionType;
                }
                if (plugin.getMaterialTags().containsKey(material)) {
                    plugin.getConfig().set("blocks.#%s.autoProtect".formatted(plugin.getMaterialTags().get(material).getKey().getKey()), boltProtectionType);
                } else {
                    plugin.getConfig().set("blocks.%s.autoProtect".formatted(material.name().toLowerCase()), boltProtectionType);
                }
            });
            plugin.saveConfig();
            plugin.reload();
        }
    }

    private void convertDoors() {
        final FileConfiguration lwcDoorsConfig = YamlConfiguration.loadConfiguration(plugin.getPluginsPath().resolve("LWC/doors.yml").toFile());
        final ConfigurationSection doors = lwcDoorsConfig.getConfigurationSection("doors");
        if (doors == null) {
            return;
        }
        final boolean enabled = doors.getBoolean("enabled", false);
        final boolean doubleDoors = doors.getBoolean("doubleDoors", false);
        final String action = doors.getString("action", "toggle");
        final int interval = doors.getInt("interval", 3);
        final boolean isOpenAndClose = "openAndClose".equals(action);
        if (!enabled && !doubleDoors && !isOpenAndClose) {
            return;
        }
        plugin.getConfig().set("doors.open-iron", enabled);
        plugin.getConfig().set("doors.open-double", doubleDoors);
        plugin.getConfig().set("doors.close-after", interval);
        plugin.getConfig().set("access.autoclose.require-permission", true);
        plugin.getConfig().set("access.autoclose.allows", List.of(Permission.AUTO_CLOSE));
        plugin.getConfig().set("sources.door.require-permission", true);
        plugin.saveConfig();
        plugin.reload();
    }
}

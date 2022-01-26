package org.popcraft.bolt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.command.impl.DebugCommand;
import org.popcraft.bolt.command.impl.InfoCommand;
import org.popcraft.bolt.command.impl.LockCommand;
import org.popcraft.bolt.command.impl.ModifyCommand;
import org.popcraft.bolt.command.impl.ReportCommand;
import org.popcraft.bolt.command.impl.UnlockCommand;
import org.popcraft.bolt.event.AccessEvents;
import org.popcraft.bolt.event.EnvironmentEvents;
import org.popcraft.bolt.event.adapter.PlayerRecipeBookClickListener;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.store.SQLiteStore;
import org.popcraft.bolt.store.Store;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.util.lang.Translation;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoltPlugin extends JavaPlugin {
    private static final String COMMAND_PERMISSION_KEY = "bolt.command.";
    private final Bolt bolt = new Bolt(new SQLiteStore());
    private final Map<String, BoltCommand> commands = new HashMap<>();
    private YamlConfigurationLoader configurationLoader;
    private ConfigurationNode configurationRootNode;

    @Override
    public void onEnable() {
        loadConfiguration();
        registerAccessTypes();
        BoltComponents.enable(this);
        registerEvents();
        registerCommands();
        listAllBlockProtections(bolt.getStore());
    }

    private void loadConfiguration() {
        configurationLoader = YamlConfigurationLoader.builder()
                .path(getDataFolder().toPath().resolve("config.yml"))
                .build();
        try {
            configurationRootNode = configurationLoader.load();
            if (!configurationRootNode.hasChild("version")) {
                final YamlConfigurationLoader defaultLoader = YamlConfigurationLoader.builder()
                        .url(getClassLoader().getResource("config.yml"))
                        .build();
                final ConfigurationNode configurationDefaultRootNode = defaultLoader.load();
                configurationRootNode.mergeFrom(configurationDefaultRootNode);
                configurationLoader.save(configurationRootNode);
            }
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

    private void saveConfiguration() {
        try {
            configurationLoader.save(configurationRootNode);
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

    private void registerAccessTypes() {
        final ConfigurationNode accessTypesMapNode = configurationRootNode.node("access-types");
        if (accessTypesMapNode.isMap()) {
            accessTypesMapNode.childrenMap().forEach((key, permissionsNode) -> {
                try {
                    final List<String> permissions = permissionsNode.getList(String.class);
                    if (key instanceof String type && permissions != null) {
                        bolt.getAccessRegistry().register(type, new HashSet<>(permissions));
                    }
                } catch (SerializationException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void registerEvents() {
        final AccessEvents accessEvents = new AccessEvents(this);
        getServer().getPluginManager().registerEvents(accessEvents, this);
        getServer().getPluginManager().registerEvents(new PlayerRecipeBookClickListener(accessEvents::onPlayerRecipeBookClick), this);
        getServer().getPluginManager().registerEvents(new EnvironmentEvents(this), this);
    }

    private void registerCommands() {
        commands.put("debug", new DebugCommand(this));
        commands.put("info", new InfoCommand(this));
        commands.put("lock", new LockCommand(this));
        commands.put("modify", new ModifyCommand(this));
        commands.put("report", new ReportCommand(this));
        commands.put("unlock", new UnlockCommand(this));
    }

    @Override
    public void onDisable() {
        BoltComponents.disable();
        HandlerList.unregisterAll(this);
        commands.clear();
        saveConfiguration();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0 && commands.containsKey(args[0].toLowerCase())) {
            if (sender.hasPermission(COMMAND_PERMISSION_KEY + args[0].toLowerCase())) {
                commands.get(args[0].toLowerCase()).execute(sender, new Arguments(Arrays.copyOfRange(args, 1, args.length)));
            } else {
                BoltComponents.sendMessage(sender, Translation.COMMAND_NO_PERMISSION);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length < 1) {
            return Collections.emptyList();
        }
        final List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            commands.keySet().stream().filter(name -> sender.hasPermission(COMMAND_PERMISSION_KEY + name)).forEach(suggestions::add);
        } else if (commands.containsKey(args[0].toLowerCase()) && sender.hasPermission(COMMAND_PERMISSION_KEY + args[0].toLowerCase())) {
            suggestions.addAll(commands.get(args[0].toLowerCase()).suggestions());
        }
        return suggestions.stream()
                .filter(s -> s.toLowerCase().contains(args[args.length - 1].toLowerCase()))
                .toList();
    }

    public Bolt getBolt() {
        return bolt;
    }

    public ConfigurationNode getConfiguration() {
        return configurationRootNode;
    }

    public PlayerMeta playerMeta(final Player player) {
        return playerMeta(player.getUniqueId());
    }

    public PlayerMeta playerMeta(final UUID uuid) {
        return bolt.getPlayerMeta(uuid);
    }

    private void listAllBlockProtections(final Store store) {
        final List<BlockProtection> sqliteProtections = store.loadBlockProtections();
        sqliteProtections.forEach(protection -> getLogger().info(() -> new ProtectionData(
                UUID.randomUUID(),
                protection.getOwner(),
                protection.getType(),
                protection.getAccessList(),
                protection.getBlock(),
                protection.getWorld(),
                protection.getX(),
                protection.getY(),
                protection.getZ()
        ).toString()));
    }

    private record ProtectionData(UUID id, UUID owner, String type, Map<Source, String> accessList, String block,
                                  String world, int x, int y, int z) {
    }
}

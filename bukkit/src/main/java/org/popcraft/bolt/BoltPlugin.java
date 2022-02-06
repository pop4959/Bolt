package org.popcraft.bolt;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.command.impl.DebugCommand;
import org.popcraft.bolt.command.impl.EditCommand;
import org.popcraft.bolt.command.impl.InfoCommand;
import org.popcraft.bolt.command.impl.LockCommand;
import org.popcraft.bolt.command.impl.PersistCommand;
import org.popcraft.bolt.command.impl.ReportCommand;
import org.popcraft.bolt.command.impl.UnlockCommand;
import org.popcraft.bolt.data.SQLiteStore;
import org.popcraft.bolt.data.SimpleProtectionCache;
import org.popcraft.bolt.listeners.BlockListener;
import org.popcraft.bolt.listeners.EntityListener;
import org.popcraft.bolt.listeners.InventoryListener;
import org.popcraft.bolt.listeners.PlayerListener;
import org.popcraft.bolt.listeners.adapter.PlayerRecipeBookClickListener;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.lang.Translation;
import org.popcraft.bolt.util.matcher.Match;
import org.popcraft.bolt.util.matcher.block.AmethystClusterMatcher;
import org.popcraft.bolt.util.matcher.block.ArmorStandMatcher;
import org.popcraft.bolt.util.matcher.block.BannerMatcher;
import org.popcraft.bolt.util.matcher.block.BedMatcher;
import org.popcraft.bolt.util.matcher.block.BellMatcher;
import org.popcraft.bolt.util.matcher.block.BigDripleafMatcher;
import org.popcraft.bolt.util.matcher.block.BlockMatcher;
import org.popcraft.bolt.util.matcher.block.CakeMatcher;
import org.popcraft.bolt.util.matcher.block.CarpetMatcher;
import org.popcraft.bolt.util.matcher.block.ChestMatcher;
import org.popcraft.bolt.util.matcher.block.ChorusMatcher;
import org.popcraft.bolt.util.matcher.block.CocoaMatcher;
import org.popcraft.bolt.util.matcher.block.CoralMatcher;
import org.popcraft.bolt.util.matcher.block.CropsMatcher;
import org.popcraft.bolt.util.matcher.block.DeadBushMatcher;
import org.popcraft.bolt.util.matcher.block.DoorMatcher;
import org.popcraft.bolt.util.matcher.block.FarmlandMatcher;
import org.popcraft.bolt.util.matcher.block.GrassMatcher;
import org.popcraft.bolt.util.matcher.block.HangingVineMatcher;
import org.popcraft.bolt.util.matcher.block.ItemFrameMatcher;
import org.popcraft.bolt.util.matcher.block.LadderMatcher;
import org.popcraft.bolt.util.matcher.block.LanternMatcher;
import org.popcraft.bolt.util.matcher.block.LeashHitchMatcher;
import org.popcraft.bolt.util.matcher.block.MossCarpetMatcher;
import org.popcraft.bolt.util.matcher.block.MushroomMatcher;
import org.popcraft.bolt.util.matcher.block.NetherWartMatcher;
import org.popcraft.bolt.util.matcher.block.PaintingMatcher;
import org.popcraft.bolt.util.matcher.block.PortalMatcher;
import org.popcraft.bolt.util.matcher.block.PressurePlateMatcher;
import org.popcraft.bolt.util.matcher.block.RailMatcher;
import org.popcraft.bolt.util.matcher.block.RedstoneWireMatcher;
import org.popcraft.bolt.util.matcher.block.SaplingMatcher;
import org.popcraft.bolt.util.matcher.block.ScaffoldingMatcher;
import org.popcraft.bolt.util.matcher.block.SeaPickleMatcher;
import org.popcraft.bolt.util.matcher.block.SignMatcher;
import org.popcraft.bolt.util.matcher.block.SmallDripleafMatcher;
import org.popcraft.bolt.util.matcher.block.SmallFlowerMatcher;
import org.popcraft.bolt.util.matcher.block.SnowMatcher;
import org.popcraft.bolt.util.matcher.block.SweetBerryBushMatcher;
import org.popcraft.bolt.util.matcher.block.SwitchMatcher;
import org.popcraft.bolt.util.matcher.block.TallFlowerMatcher;
import org.popcraft.bolt.util.matcher.block.TallGrassMatcher;
import org.popcraft.bolt.util.matcher.block.TechnicalPistonMatcher;
import org.popcraft.bolt.util.matcher.block.TorchMatcher;
import org.popcraft.bolt.util.matcher.block.TrapdoorMatcher;
import org.popcraft.bolt.util.matcher.block.TripwireHookMatcher;
import org.popcraft.bolt.util.matcher.block.UprootMatcher;
import org.popcraft.bolt.util.matcher.block.VineMatcher;
import org.popcraft.bolt.util.matcher.entity.EntityMatcher;
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
import java.util.Optional;
import java.util.UUID;

public class BoltPlugin extends JavaPlugin {
    private static final String COMMAND_PERMISSION_KEY = "bolt.command.";
    private static final List<BlockMatcher> BLOCK_MATCHERS = List.of(new ArmorStandMatcher(), new BannerMatcher(),
            new BedMatcher(), new ChestMatcher(), new DoorMatcher(), new LeashHitchMatcher(),
            new PressurePlateMatcher(), new RailMatcher(), new SignMatcher(), new SwitchMatcher(),
            new TrapdoorMatcher(), new CropsMatcher(), new FarmlandMatcher(), new UprootMatcher(),
            new BellMatcher(), new TorchMatcher(), new LanternMatcher(), new LadderMatcher(),
            new CocoaMatcher(), new TripwireHookMatcher(), new AmethystClusterMatcher(), new SaplingMatcher(),
            new TechnicalPistonMatcher(), new ItemFrameMatcher(), new PaintingMatcher(), new HangingVineMatcher(),
            new SeaPickleMatcher(), new VineMatcher(), new CakeMatcher(), new CoralMatcher(),
            new RedstoneWireMatcher(), new SmallFlowerMatcher(), new TallFlowerMatcher(), new SnowMatcher(),
            new CarpetMatcher(), new PortalMatcher(), new SmallDripleafMatcher(), new BigDripleafMatcher(),
            new ScaffoldingMatcher(), new MossCarpetMatcher(), new MushroomMatcher(), new NetherWartMatcher(),
            new SweetBerryBushMatcher(), new ChorusMatcher(), new GrassMatcher(), new TallGrassMatcher(),
            new DeadBushMatcher());
    private static final List<EntityMatcher> ENTITY_MATCHERS = List.of();
    private final Bolt bolt = new Bolt(new SimpleProtectionCache(new SQLiteStore()));
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
        final PluginManager pluginManager = getServer().getPluginManager();
        final BlockListener blockListener = new BlockListener(this);
        pluginManager.registerEvents(blockListener, this);
        pluginManager.registerEvents(new PlayerRecipeBookClickListener(blockListener::onPlayerRecipeBookClick), this);
        pluginManager.registerEvents(new EntityListener(this), this);
        pluginManager.registerEvents(new InventoryListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);
    }

    private void registerCommands() {
        commands.put("debug", new DebugCommand(this));
        commands.put("edit", new EditCommand(this));
        commands.put("info", new InfoCommand(this));
        commands.put("lock", new LockCommand(this));
        commands.put("persist", new PersistCommand(this));
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

    public PlayerMeta playerMeta(final Player player) {
        return playerMeta(player.getUniqueId());
    }

    public PlayerMeta playerMeta(final UUID uuid) {
        return bolt.getPlayerMeta(uuid);
    }

    public Optional<Protection> findProtection(final Block block) {
        final Protection protection = bolt.getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
        return Optional.ofNullable(protection != null ? protection : matchProtection(block));
    }

    public Optional<Protection> findProtection(final Entity entity) {
        final Protection protection = bolt.getStore().loadEntityProtection(entity.getUniqueId());
        return Optional.ofNullable(protection != null ? protection : matchProtection(entity));
    }

    @SuppressWarnings("java:S2583")
    public void saveProtection(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            bolt.getStore().saveBlockProtection(blockProtection);
        } else if (protection instanceof final EntityProtection entityProtection) {
            bolt.getStore().saveEntityProtection(entityProtection);
        }
    }

    @SuppressWarnings("java:S2583")
    public void removeProtection(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            bolt.getStore().removeBlockProtection(blockProtection);
        } else if (protection instanceof final EntityProtection entityProtection) {
            bolt.getStore().removeEntityProtection(entityProtection);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canAccessProtection(final UUID uuid, final Protection protection, final String... permissions) {
        return bolt.getAccessManager().hasAccess(playerMeta(uuid), protection, permissions);
    }

    public boolean canAccessProtection(final Player player, final Protection protection, final String... permissions) {
        return bolt.getAccessManager().hasAccess(playerMeta(player), protection, permissions);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canAccessBlock(final Player player, final Block block, final String... permissions) {
        return findProtection(block).map(protection -> canAccessProtection(player, protection, permissions)).orElse(true);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canAccessEntity(final Player player, final Entity entity, final String... permissions) {
        return findProtection(entity).map(protection -> canAccessProtection(player, protection, permissions)).orElse(true);
    }

    private Protection matchProtection(final Block block) {
        for (final BlockMatcher blockMatcher : BLOCK_MATCHERS) {
            if (blockMatcher.canMatch(block)) {
                final Optional<Match> optionalMatch = blockMatcher.findMatch(block);
                if (optionalMatch.isPresent()) {
                    final Match match = optionalMatch.get();
                    for (final Block matchBlock : match.blocks()) {
                        final BlockProtection protection = bolt.getStore().loadBlockProtection(BukkitAdapter.blockLocation(matchBlock));
                        if (protection != null) {
                            return protection;
                        }
                    }
                    for (final Entity matchEntity : match.entities()) {
                        final EntityProtection protection = bolt.getStore().loadEntityProtection(matchEntity.getUniqueId());
                        if (protection != null) {
                            return protection;
                        }
                    }
                }
            }
        }
        return null;
    }

    private Protection matchProtection(final Entity entity) {
        for (final EntityMatcher entityMatcher : ENTITY_MATCHERS) {
            if (entityMatcher.canMatch(entity)) {
                final Optional<Match> optionalMatch = entityMatcher.findMatch(entity);
                if (optionalMatch.isPresent()) {
                    final Match match = optionalMatch.get();
                    for (final Block matchBlock : match.blocks()) {
                        final BlockProtection protection = bolt.getStore().loadBlockProtection(BukkitAdapter.blockLocation(matchBlock));
                        if (protection != null) {
                            return protection;
                        }
                    }
                    for (final Entity matchEntity : match.entities()) {
                        final EntityProtection protection = bolt.getStore().loadEntityProtection(matchEntity.getUniqueId());
                        if (protection != null) {
                            return protection;
                        }
                    }
                }
            }
        }
        return null;
    }
}

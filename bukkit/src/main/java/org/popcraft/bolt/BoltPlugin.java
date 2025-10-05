package org.popcraft.bolt;

import net.kyori.event.EventBus;
import net.kyori.event.SimpleEventBus;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.access.AccessList;
import org.popcraft.bolt.access.AccessRegistry;
import org.popcraft.bolt.access.DefaultAccess;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.command.callback.CallbackManager;
import org.popcraft.bolt.command.impl.AdminCommand;
import org.popcraft.bolt.command.impl.CallbackCommand;
import org.popcraft.bolt.command.impl.EditCommand;
import org.popcraft.bolt.command.impl.GroupCommand;
import org.popcraft.bolt.command.impl.HelpCommand;
import org.popcraft.bolt.command.impl.InfoCommand;
import org.popcraft.bolt.command.impl.LockCommand;
import org.popcraft.bolt.command.impl.ModeCommand;
import org.popcraft.bolt.command.impl.ModifyCommand;
import org.popcraft.bolt.command.impl.PasswordCommand;
import org.popcraft.bolt.command.impl.TransferCommand;
import org.popcraft.bolt.command.impl.TrustCommand;
import org.popcraft.bolt.command.impl.UnlockCommand;
import org.popcraft.bolt.data.ProfileCache;
import org.popcraft.bolt.data.SQLStore;
import org.popcraft.bolt.data.SimpleProfileCache;
import org.popcraft.bolt.data.SimpleProtectionCache;
import org.popcraft.bolt.data.migration.lwc.ConfigMigration;
import org.popcraft.bolt.data.migration.lwc.TrustMigration;
import org.popcraft.bolt.event.Event;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.lang.Translator;
import org.popcraft.bolt.listeners.BlockListener;
import org.popcraft.bolt.listeners.EntityListener;
import org.popcraft.bolt.listeners.InventoryListener;
import org.popcraft.bolt.listeners.PlayerListener;
import org.popcraft.bolt.listeners.adapter.AnvilDamagedListener;
import org.popcraft.bolt.listeners.adapter.BlockBreakBlockEventListener;
import org.popcraft.bolt.listeners.adapter.BlockDestroyListener;
import org.popcraft.bolt.listeners.adapter.BlockPreDispenseListener;
import org.popcraft.bolt.listeners.adapter.ItemTransportingEntityValidateTargetEventListener;
import org.popcraft.bolt.listeners.adapter.PlayerRecipeBookClickListener;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.matcher.block.AmethystClusterMatcher;
import org.popcraft.bolt.matcher.block.BannerMatcher;
import org.popcraft.bolt.matcher.block.BedMatcher;
import org.popcraft.bolt.matcher.block.BellMatcher;
import org.popcraft.bolt.matcher.block.BigDripleafMatcher;
import org.popcraft.bolt.matcher.block.BlockMatcher;
import org.popcraft.bolt.matcher.block.CakeMatcher;
import org.popcraft.bolt.matcher.block.CarpetMatcher;
import org.popcraft.bolt.matcher.block.ChestMatcher;
import org.popcraft.bolt.matcher.block.ChorusMatcher;
import org.popcraft.bolt.matcher.block.CocoaMatcher;
import org.popcraft.bolt.matcher.block.CoralMatcher;
import org.popcraft.bolt.matcher.block.CropsMatcher;
import org.popcraft.bolt.matcher.block.DeadBushMatcher;
import org.popcraft.bolt.matcher.block.DoorMatcher;
import org.popcraft.bolt.matcher.block.FarmlandMatcher;
import org.popcraft.bolt.matcher.block.FireMatcher;
import org.popcraft.bolt.matcher.block.FrogspawnMatcher;
import org.popcraft.bolt.matcher.block.GrassMatcher;
import org.popcraft.bolt.matcher.block.HangingRootsMatcher;
import org.popcraft.bolt.matcher.block.HangingSignMatcher;
import org.popcraft.bolt.matcher.block.HangingVineMatcher;
import org.popcraft.bolt.matcher.block.ItemFrameMatcher;
import org.popcraft.bolt.matcher.block.LadderMatcher;
import org.popcraft.bolt.matcher.block.LanternMatcher;
import org.popcraft.bolt.matcher.block.LeashKnotMatcher;
import org.popcraft.bolt.matcher.block.LilyPadMatcher;
import org.popcraft.bolt.matcher.block.MangrovePropaguleMatcher;
import org.popcraft.bolt.matcher.block.MossCarpetMatcher;
import org.popcraft.bolt.matcher.block.MultipleFacingMatcher;
import org.popcraft.bolt.matcher.block.MushroomMatcher;
import org.popcraft.bolt.matcher.block.NetherWartMatcher;
import org.popcraft.bolt.matcher.block.PaintingMatcher;
import org.popcraft.bolt.matcher.block.PinkPetalsMatcher;
import org.popcraft.bolt.matcher.block.PointedDripstoneMatcher;
import org.popcraft.bolt.matcher.block.PortalMatcher;
import org.popcraft.bolt.matcher.block.PressurePlateMatcher;
import org.popcraft.bolt.matcher.block.RailMatcher;
import org.popcraft.bolt.matcher.block.RedstoneWireMatcher;
import org.popcraft.bolt.matcher.block.RepeaterMatcher;
import org.popcraft.bolt.matcher.block.SaplingMatcher;
import org.popcraft.bolt.matcher.block.ScaffoldingMatcher;
import org.popcraft.bolt.matcher.block.SeaPickleMatcher;
import org.popcraft.bolt.matcher.block.SignMatcher;
import org.popcraft.bolt.matcher.block.SmallDripleafMatcher;
import org.popcraft.bolt.matcher.block.SmallFlowerMatcher;
import org.popcraft.bolt.matcher.block.SnowMatcher;
import org.popcraft.bolt.matcher.block.SoulFireMatcher;
import org.popcraft.bolt.matcher.block.SporeBlossomMatcher;
import org.popcraft.bolt.matcher.block.SweetBerryBushMatcher;
import org.popcraft.bolt.matcher.block.SwitchMatcher;
import org.popcraft.bolt.matcher.block.TallFlowerMatcher;
import org.popcraft.bolt.matcher.block.TallGrassMatcher;
import org.popcraft.bolt.matcher.block.TechnicalPistonMatcher;
import org.popcraft.bolt.matcher.block.TorchMatcher;
import org.popcraft.bolt.matcher.block.TripwireHookMatcher;
import org.popcraft.bolt.matcher.block.UprootMatcher;
import org.popcraft.bolt.matcher.block.VineMatcher;
import org.popcraft.bolt.matcher.entity.EntityMatcher;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.GroupSourceTransformer;
import org.popcraft.bolt.source.PasswordSourceTransformer;
import org.popcraft.bolt.source.PlayerSourceTransformer;
import org.popcraft.bolt.source.PlayerSourceResolver;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceTransformer;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceTypeRegistry;
import org.popcraft.bolt.source.SourceTypes;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.BukkitPlayerResolver;
import org.popcraft.bolt.util.EnumUtil;
import org.popcraft.bolt.util.Group;
import org.popcraft.bolt.util.Mode;
import org.popcraft.bolt.util.PaperUtil;
import org.popcraft.bolt.util.ProtectableConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BoltPlugin extends JavaPlugin implements BoltAPI {
    public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("boltDebug", "false"));
    private static final String COMMAND_PERMISSION_KEY = "bolt.command.";
    private static final BlockMatcher CHEST_MATCHER = new ChestMatcher();
    private static final List<BlockMatcher> BLOCK_MATCHERS = List.of(CHEST_MATCHER,
            new BannerMatcher(), new BedMatcher(), new DoorMatcher(), new LeashKnotMatcher(),
            new PressurePlateMatcher(), new RailMatcher(), new SignMatcher(), new SwitchMatcher(),
            new CropsMatcher(), new FarmlandMatcher(), new UprootMatcher(),
            new BellMatcher(), new TorchMatcher(), new LanternMatcher(), new LadderMatcher(),
            new CocoaMatcher(), new TripwireHookMatcher(), new AmethystClusterMatcher(), new SaplingMatcher(),
            new TechnicalPistonMatcher(), new ItemFrameMatcher(), new PaintingMatcher(), new HangingVineMatcher(),
            new SeaPickleMatcher(), new VineMatcher(), new CakeMatcher(), new CoralMatcher(),
            new RedstoneWireMatcher(), new SmallFlowerMatcher(), new TallFlowerMatcher(), new SnowMatcher(),
            new CarpetMatcher(), new PortalMatcher(), new SmallDripleafMatcher(), new BigDripleafMatcher(),
            new ScaffoldingMatcher(), new MossCarpetMatcher(), new MushroomMatcher(), new NetherWartMatcher(),
            new SweetBerryBushMatcher(), new ChorusMatcher(), new GrassMatcher(), new TallGrassMatcher(),
            new DeadBushMatcher(), new HangingRootsMatcher(), new PointedDripstoneMatcher(), new FireMatcher(),
            new LilyPadMatcher(), new RepeaterMatcher(), new SporeBlossomMatcher(),
            new SoulFireMatcher(), new FrogspawnMatcher(), new MangrovePropaguleMatcher(), new MultipleFacingMatcher(),
            new HangingSignMatcher(), new PinkPetalsMatcher());
    private static final List<EntityMatcher> ENTITY_MATCHERS = List.of();
    private static final Source ADMIN_PERMISSION_SOURCE = Source.of(SourceTypes.PERMISSION, "bolt.admin");
    private static final Source MOD_PERMISSION_SOURCE = Source.of(SourceTypes.PERMISSION, "bolt.mod");
    private final List<BlockMatcher> enabledBlockMatchers = new ArrayList<>();
    private final List<EntityMatcher> enabledEntityMatchers = new ArrayList<>();
    private final Map<String, BoltCommand> commands = new HashMap<>();
    private final Path profileCachePath = getDataPath().resolve("profiles");
    private final ProfileCache profileCache = new SimpleProfileCache(profileCachePath);
    private final Map<Material, ProtectableConfig> protectableBlocks = new HashMap<>();
    private final Map<EntityType, ProtectableConfig> protectableEntities = new HashMap<>();
    private final Map<Material, Tag<Material>> materialTags = new HashMap<>();
    private final Set<Mode> defaultModes = new HashSet<>();
    private String defaultProtectionType = "private";
    private String defaultAccessType = "normal";
    private Map<String, SourceTransformer> sourceTransformers = new HashMap<>();
    private boolean useActionBar;
    private boolean doors;
    private boolean doorsOpenIron;
    private boolean doorsOpenDouble;
    private int doorsCloseAfter;
    private boolean doorsFixPlugins;
    private Bolt bolt;
    private CallbackManager callbackManager;
    private EventBus<Event> eventBus;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        final SQLStore.Configuration databaseConfiguration = new SQLStore.Configuration(
                getConfig().getString("database.type", "sqlite").toLowerCase(),
                getConfig().getString("database.path", "%s/Bolt/bolt.db".formatted(getPluginsPath().toFile().getName())),
                getConfig().getString("database.hostname", ""),
                getConfig().getString("database.database", ""),
                getConfig().getString("database.username", ""),
                getConfig().getString("database.password", ""),
                getConfig().getString("database.prefix", ""),
                Optional.ofNullable(getConfig().getConfigurationSection("database.properties"))
                        .map(section -> section.getKeys(false))
                        .stream()
                        .collect(Collectors.toMap(String::valueOf, key -> getConfig().getString("database.properties." + key, "")))
        );
        this.bolt = new Bolt(new SimpleProtectionCache(new SQLStore(databaseConfiguration)));
        reload();
        BoltComponents.enable();
        registerEvents();
        registerCommands();
        this.callbackManager = new CallbackManager(this);
        this.eventBus = new SimpleEventBus<>(Event.class);
        profileCache.load();
        final Metrics metrics = new Metrics(this, 17711);
        registerCustomCharts(metrics, databaseConfiguration);
        new ConfigMigration(this).convert();
        // Future: Move this into LWC Migration
        new TrustMigration(this).convert();
        getServer().getServicesManager().register(BoltAPI.class, this, this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        BoltComponents.disable();
        HandlerList.unregisterAll(this);
        commands.clear();
        getLogger().info(() -> "Flushing protection updates (%d)".formatted(bolt.getStore().pendingSave()));
        bolt.getStore().flush().join();
        getServer().getServicesManager().unregisterAll(this);
    }

    public void reload() {
        reloadConfig();
        Translator.loadAllTranslations(getDataPath(), getConfig().getString("language", "en"), getConfig().getBoolean("settings.per-player-locale", true));
        this.useActionBar = getConfig().getBoolean("settings.use-action-bar", false);
        this.doors = getConfig().getConfigurationSection("doors") != null;
        this.doorsOpenIron = getConfig().getBoolean("doors.open-iron", false);
        this.doorsOpenDouble = getConfig().getBoolean("doors.open-double", false);
        this.doorsCloseAfter = getConfig().getInt("doors.close-after", 0);
        this.doorsFixPlugins = getConfig().getBoolean("doors.fix-plugins", false);
        registerAccessTypes();
        registerProtectableAccess();
        registerAccessSources();
        initializeMatchers();
        loadDefaultModes();
        registerDefaultSourceTransformers();
    }

    private void registerCustomCharts(final Metrics metrics, final SQLStore.Configuration databaseConfiguration) {
        metrics.addCustomChart(new SimplePie("config_language", Translator::selected));
        metrics.addCustomChart(new SimplePie("config_database", databaseConfiguration::type));
        metrics.addCustomChart(new AdvancedPie("config_protections", () -> {
            final Map<String, Integer> map = new HashMap<>();
            bolt.getAccessRegistry().protectionTypes().forEach(type -> map.put(type, 1));
            return map;
        }));
        metrics.addCustomChart(new AdvancedPie("config_access", () -> {
            final Map<String, Integer> map = new HashMap<>();
            bolt.getAccessRegistry().accessTypes().forEach(type -> map.put(type, 1));
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("config_blocks", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Optional.ofNullable(getConfig().getConfigurationSection("blocks"))
                    .ifPresent(section -> {
                        final Set<String> types = section.getKeys(false);
                        types.forEach(type -> map.put(type, Map.of(section.getString("%s.autoProtect".formatted(type), "false"), 1)));
                    });
            return map;
        }));
        metrics.addCustomChart(new DrilldownPie("config_entities", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            Optional.ofNullable(getConfig().getConfigurationSection("entities"))
                    .ifPresent(section -> {
                        final Set<String> types = section.getKeys(false);
                        types.forEach(type -> map.put(type, Map.of(section.getString("%s.autoProtect".formatted(type), "false"), 1)));
                    });
            return map;
        }));
        metrics.addCustomChart(new SimplePie("protections_blocks", () -> String.valueOf((int) Math.ceil(bolt.getStore().loadBlockProtections().join().size() / 1000f) * 1000)));
        metrics.addCustomChart(new SimplePie("protections_entities", () -> String.valueOf((int) Math.ceil(bolt.getStore().loadEntityProtections().join().size() / 1000f) * 1000)));
    }

    private void registerAccessTypes() {
        bolt.getAccessRegistry().unregisterAll();
        final ConfigurationSection protections = getConfig().getConfigurationSection("protections");
        if (protections != null) {
            for (final String typeKey : protections.getKeys(false)) {
                final String type = typeKey.toLowerCase();
                final boolean requirePermission = protections.getBoolean("%s.require-permission".formatted(type), false);
                final List<String> allows = protections.getStringList("%s.allows".formatted(type));
                final List<String> permissions = allows.isEmpty() ? protections.getStringList(type) : allows;
                bolt.getAccessRegistry().registerProtectionType(type, requirePermission, new HashSet<>(permissions));
                final boolean isDefault = protections.getBoolean("%s.default".formatted(type), false);
                if (isDefault) {
                    defaultProtectionType = type;
                }
            }
        }
        final ConfigurationSection access = getConfig().getConfigurationSection("access");
        if (access != null) {
            for (final String typeKey : access.getKeys(false)) {
                final String type = typeKey.toLowerCase();
                final boolean requirePermission = access.getBoolean("%s.require-permission".formatted(type), false);
                final List<String> allows = access.getStringList("%s.allows".formatted(type));
                final List<String> permissions = allows.isEmpty() ? access.getStringList(type) : allows;
                bolt.getAccessRegistry().registerAccessType(type, requirePermission, new HashSet<>(permissions));
                final boolean isDefault = access.getBoolean("%s.default".formatted(type), false);
                if (isDefault) {
                    defaultAccessType = type;
                }
            }
        }
    }

    private void registerProtectableAccess() {
        protectableBlocks.clear();
        protectableEntities.clear();
        if (DEBUG) {
            for (final Material material : Registry.MATERIAL) {
                if (material.isBlock()) {
                    protectableBlocks.put(material, new ProtectableConfig(bolt.getAccessRegistry().getProtectionByType(defaultAccessType).orElse(null), false, false));
                }
            }
            for (final EntityType entity : EntityType.values()) {
                protectableEntities.put(entity, new ProtectableConfig(bolt.getAccessRegistry().getProtectionByType(defaultAccessType).orElse(null), false, false));
            }
        }
        final ConfigurationSection blocks = getConfig().getConfigurationSection("blocks");
        if (blocks != null) {
            for (final String key : blocks.getKeys(false)) {
                final String autoProtectType = blocks.getString("%s.autoProtect".formatted(key), "false");
                final Access defaultAccess = bolt.getAccessRegistry().getProtectionByType(autoProtectType).orElse(null);
                final boolean lockPermission = blocks.getBoolean("%s.lockPermission".formatted(key), false);
                final boolean autoProtectPermission = blocks.getBoolean("%s.autoProtectPermission".formatted(key), false);
                if (key.startsWith("#")) {
                    final Tag<Material> tag = resolveTagProtectableAccess(Tag.REGISTRY_BLOCKS, Material.class, key.substring(1));
                    if (tag == null) {
                        getLogger().warning(() -> "Invalid block tag defined in config: %s. Skipping.".formatted(key));
                        continue;
                    }
                    tag.getValues().forEach(block -> {
                        protectableBlocks.put(block, new ProtectableConfig(defaultAccess, lockPermission, autoProtectPermission));
                        materialTags.put(block, tag);
                    });
                } else {
                    EnumUtil.valueOf(Material.class, key.toUpperCase()).filter(Material::isBlock).ifPresentOrElse(block -> protectableBlocks.put(block, new ProtectableConfig(defaultAccess, lockPermission, autoProtectPermission)), () -> getLogger().warning(() -> "Invalid block defined in config: %s. Skipping.".formatted(key)));
                }
            }
        }
        final ConfigurationSection entities = getConfig().getConfigurationSection("entities");
        if (entities != null) {
            for (final String key : entities.getKeys(false)) {
                final String autoProtectType = entities.getString("%s.autoProtect".formatted(key), "false");
                final Access defaultAccess = bolt.getAccessRegistry().getProtectionByType(autoProtectType).orElse(null);
                final boolean lockPermission = entities.getBoolean("%s.lockPermission".formatted(key), false);
                final boolean autoProtectPermission = entities.getBoolean("%s.autoProtectPermission".formatted(key), false);
                if (key.startsWith("#")) {
                    final Tag<EntityType> tag = resolveTagProtectableAccess(Tag.REGISTRY_ENTITY_TYPES, EntityType.class, key.substring(1));
                    if (tag == null) {
                        getLogger().warning(() -> "Invalid entity tag defined in config: %s. Skipping.".formatted(key));
                        continue;
                    }
                    tag.getValues().forEach(entity -> protectableEntities.put(entity, new ProtectableConfig(defaultAccess, lockPermission, autoProtectPermission)));
                } else {
                    EnumUtil.valueOf(EntityType.class, key.toUpperCase()).ifPresentOrElse(entity -> protectableEntities.put(entity, new ProtectableConfig(defaultAccess, lockPermission, autoProtectPermission)), () -> getLogger().warning(() -> "Invalid entity defined in config: %s. Skipping.".formatted(key)));
                }
            }
        }
    }

    private void registerAccessSources() {
        final SourceTypeRegistry sourceTypeRegistry = bolt.getSourceTypeRegistry();
        sourceTypeRegistry.unregisterAll();
        final ConfigurationSection sources = getConfig().getConfigurationSection("sources");
        if (sources == null) {
            return;
        }
        for (final String source : sources.getKeys(false)) {
            final boolean requirePermission = sources.getBoolean("%s.require-permission".formatted(source), false);
            final boolean unique = sources.getBoolean("%s.unique".formatted(source), false);
            sourceTypeRegistry.registerSourceType(source, requirePermission, unique);
        }
        if (sourceTypeRegistry.sourceTypes().isEmpty()) {
            sourceTypeRegistry.registerSourceType(SourceTypes.PLAYER, false, false);
            sourceTypeRegistry.registerSourceType(SourceTypes.PASSWORD, false, false);
            sourceTypeRegistry.registerSourceType(SourceTypes.GROUP, false, false);
            sourceTypeRegistry.registerSourceType(SourceTypes.PERMISSION, true, false);
        }
    }

    private <T extends Keyed> Tag<T> resolveTagProtectableAccess(final String registry, final Class<T> clazz, final String name) {
        final NamespacedKey tagKey = NamespacedKey.fromString(name);
        return tagKey == null ? null : getServer().getTag(registry, tagKey, clazz);
    }

    private void loadDefaultModes() {
        defaultModes.clear();
        final List<String> modes = getConfig().getStringList("settings.default-modes");
        for (String modeName : modes) {
            final Mode mode;
            try {
                mode = Mode.valueOf(modeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                getLogger().warning(() -> "Invalid default mode defined in config: %s. Skipping.".formatted(modeName));
                return;
            }

            defaultModes.add(mode);
        }
    }

    public Set<Mode> defaultModes() {
        return defaultModes;
    }

    private void initializeMatchers() {
        enabledBlockMatchers.clear();
        enabledEntityMatchers.clear();
        BLOCK_MATCHERS.forEach(blockMatcher -> blockMatcher.initialize(protectableBlocks.keySet(), protectableEntities.keySet()));
        ENTITY_MATCHERS.forEach(entityMatcher -> entityMatcher.initialize(protectableBlocks.keySet(), protectableEntities.keySet()));
        for (final BlockMatcher blockMatcher : BLOCK_MATCHERS) {
            if (blockMatcher.enabled()) {
                enabledBlockMatchers.add(blockMatcher);
            }
        }
        for (final EntityMatcher entityMatcher : ENTITY_MATCHERS) {
            if (entityMatcher.enabled()) {
                enabledEntityMatchers.add(entityMatcher);
            }
        }
    }

    private void registerEvents() {
        final PluginManager pluginManager = getServer().getPluginManager();
        final BlockListener blockListener = new BlockListener(this);
        pluginManager.registerEvents(blockListener, this);
        if (PaperUtil.isPaper()) {
            pluginManager.registerEvents(new PlayerRecipeBookClickListener(blockListener::onPlayerRecipeBookClick), this);
            pluginManager.registerEvents(new BlockPreDispenseListener(blockListener::onBlockPreDispense), this);
            pluginManager.registerEvents(new BlockDestroyListener(blockListener::onBlockDestroy), this);
            pluginManager.registerEvents(new BlockBreakBlockEventListener(blockListener::onBlockBreakBlockEvent), this);
        }
        final EntityListener entityListener = new EntityListener(this);
        pluginManager.registerEvents(entityListener, this);
        if (ItemTransportingEntityValidateTargetEventListener.canUse()) {
            pluginManager.registerEvents(new ItemTransportingEntityValidateTargetEventListener(entityListener::onItemTransportingEntityValidateTarget), this);
        }
        final InventoryListener inventoryListener = new InventoryListener(this);
        pluginManager.registerEvents(inventoryListener, this);
        if (PaperUtil.isPaper()) {
            pluginManager.registerEvents(new AnvilDamagedListener(inventoryListener::onAnvilBreak), this);
        }
        pluginManager.registerEvents(new PlayerListener(this), this);
    }

    private void registerCommands() {
        commands.put("admin", new AdminCommand(this));
        commands.put("edit", new EditCommand(this));
        commands.put("group", new GroupCommand(this));
        commands.put("help", new HelpCommand(this));
        commands.put("info", new InfoCommand(this));
        commands.put("lock", new LockCommand(this));
        commands.put("mode", new ModeCommand(this));
        commands.put("modify", new ModifyCommand(this));
        commands.put("password", new PasswordCommand(this));
        commands.put("transfer", new TransferCommand(this));
        commands.put("trust", new TrustCommand(this));
        commands.put("unlock", new UnlockCommand(this));
        commands.put("callback", new CallbackCommand(this));
    }

    public Map<String, BoltCommand> commands() {
        return commands;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        final boolean isBoltCommand = "bolt".equalsIgnoreCase(command.getName());
        final int commandStart = isBoltCommand ? 1 : 0;
        if (args.length < commandStart) {
            commands.get("help").execute(sender, new Arguments());
            return true;
        }
        final String commandKey = (isBoltCommand ? args[0] : command.getName()).toLowerCase();
        if (!commands.containsKey(commandKey)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_INVALID);
            return true;
        }
        if (!sender.hasPermission(COMMAND_PERMISSION_KEY + commandKey)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_NO_PERMISSION);
            return true;
        }
        commands.get(commandKey).execute(sender, new Arguments(Arrays.copyOfRange(args, commandStart, args.length)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        final boolean isBoltCommand = "bolt".equalsIgnoreCase(command.getName());
        final int commandStart = isBoltCommand ? 1 : 0;
        final String commandKey = (isBoltCommand ? args[0] : command.getName()).toLowerCase();
        final List<String> suggestions = new ArrayList<>();
        if (args.length == commandStart) {
            commands.entrySet().stream()
                .filter(i -> sender.hasPermission(COMMAND_PERMISSION_KEY + i.getKey()) && !i.getValue().hidden())
                .forEach(i -> suggestions.add(i.getKey()));
        } else if (commands.containsKey(commandKey) && sender.hasPermission(COMMAND_PERMISSION_KEY + commandKey)) {
            suggestions.addAll(commands.get(commandKey).suggestions(sender, new Arguments(Arrays.copyOfRange(args, commandStart, args.length))));
        }
        return suggestions.stream()
                .filter(s -> s.toLowerCase().contains(args[args.length - 1].toLowerCase()))
                .toList();
    }

    public Bolt getBolt() {
        return bolt;
    }

    public Path getDataPath() {
        return getDataFolder().toPath();
    }

    public Path getPluginsPath() {
        return getDataPath().getParent();
    }

    public boolean isUseActionBar() {
        return useActionBar;
    }

    public boolean isDoors() {
        return doors;
    }

    public boolean isDoorsOpenIron() {
        return doorsOpenIron;
    }

    public boolean isDoorsOpenDouble() {
        return doorsOpenDouble;
    }

    public int getDoorsCloseAfter() {
        return doorsCloseAfter;
    }

    public boolean getDoorsFixPlugins() {
        return doorsFixPlugins;
    }

    public ProfileCache getProfileCache() {
        return profileCache;
    }

    public CallbackManager getCallbackManager() {
        return this.callbackManager;
    }

    public EventBus<Event> getEventBus() {
        return this.eventBus;
    }

    @Override
    public <T extends Event> void registerListener(final Class<T> clazz, final Consumer<? super T> listener) {
        this.eventBus.register(clazz, listener::accept);
    }

    public BoltPlayer player(final Player player) {
        return player(player.getUniqueId());
    }

    public BoltPlayer player(final UUID uuid) {
        return bolt.getBoltPlayer(uuid);
    }

    public List<String> getPlayersOwnedGroups(final Player player) {
        return bolt.getStore().loadGroups().join().stream()
                .filter(group -> group.getOwner().equals(player.getUniqueId()))
                .map(Group::getName)
                .toList();
    }

    public ProtectableConfig getProtectableConfig(final Block block) {
        return protectableBlocks.get(block.getType());
    }

    public ProtectableConfig getProtectableConfig(final Entity entity) {
        return protectableEntities.get(entity.getType());
    }

    public String getDefaultProtectionType() {
        return defaultProtectionType;
    }

    public String getDefaultAccessType() {
        return defaultAccessType;
    }

    public BlockMatcher getChestMatcher() {
        return CHEST_MATCHER;
    }

    public Map<Material, Tag<Material>> getMaterialTags() {
        return materialTags;
    }

    @Override
    public boolean isProtectable(final Block block) {
        return DEBUG || protectableBlocks.containsKey(block.getType());
    }

    public boolean isProtectable(final Material material) {
        return DEBUG || protectableBlocks.containsKey(material);
    }

    @Override
    public boolean isProtectable(final Entity entity) {
        return DEBUG || protectableEntities.containsKey(entity.getType());
    }

    @Override
    public boolean isProtected(final Block block) {
        return findProtection(block) != null;
    }

    @Override
    public boolean isProtected(final Entity entity) {
        return findProtection(entity) != null;
    }

    @Override
    public boolean isProtectedExact(Block block) {
        return loadProtection(block) != null;
    }

    @Override
    public boolean isProtectedExact(Entity entity) {
        return loadProtection(entity) != null;
    }

    @Override
    public BlockProtection createProtection(final Block block, final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new BlockProtection(UUID.randomUUID(), owner, type, now, now, new HashMap<>(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getType().name());
    }

    @Override
    public EntityProtection createProtection(final Entity entity, final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new EntityProtection(entity.getUniqueId(), owner, type, now, now, new HashMap<>(), entity.getType().name());
    }

    @Override
    public Collection<Protection> loadProtections() {
        final Collection<Protection> protections = new ArrayList<>();
        protections.addAll(bolt.getStore().loadBlockProtections().join());
        protections.addAll(bolt.getStore().loadEntityProtections().join());
        return protections;
    }

    @Override
    public BlockProtection loadProtection(Block block) {
        final BlockLocation blockLocation = new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        return bolt.getStore().loadBlockProtection(blockLocation).join();
    }

    @Override
    public EntityProtection loadProtection(Entity entity) {
        final UUID uuid = entity.getUniqueId();
        return bolt.getStore().loadEntityProtection(uuid).join();
    }

    @Override
    public void saveProtection(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            bolt.getStore().saveBlockProtection(blockProtection);
        } else if (protection instanceof final EntityProtection entityProtection) {
            bolt.getStore().saveEntityProtection(entityProtection);
        }
    }

    @Override
    public void removeProtection(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            bolt.getStore().removeBlockProtection(blockProtection);
        } else if (protection instanceof final EntityProtection entityProtection) {
            bolt.getStore().removeEntityProtection(entityProtection);
        }
    }

    @Override
    public Protection findProtection(final Block block) {
        final Protection protection = loadProtection(block);
        return protection != null ? protection : matchProtection(block);
    }

    @Override
    public Protection findProtection(final Entity entity) {
        final Protection protection = loadProtection(entity);
        return protection != null ? protection : matchProtection(entity);
    }

    @Override
    public Collection<Protection> findProtections(final World world, final BoundingBox boundingBox) {
        final Collection<Protection> protections = new ArrayList<>();
        bolt.getStore().loadBlockProtections().join().stream()
                .filter(p -> world.getName().equals(p.getWorld()))
                .filter(p -> boundingBox.contains(p.getX(), p.getY(), p.getZ()))
                .forEach(protections::add);
        Collection<EntityProtection> entityProtections = bolt.getStore().loadEntityProtections().join();
        for (final EntityProtection entityProtection : entityProtections) {
            final Entity entity = getServer().getEntity(entityProtection.getId());
            if (entity == null) {
                continue;
            }
            if (world.getName().equals(entity.getWorld().getName()) && boundingBox.contains(entity.getBoundingBox())) {
                protections.add(entityProtection);
            }
        }
        return protections;
    }

    @Override
    public boolean canAccess(final Block block, final Player player, final String... permissions) {
        return canAccess(findProtection(block), player.getUniqueId(), permissions);
    }

    @Override
    public boolean canAccess(final Entity entity, final Player player, final String... permissions) {
        return canAccess(findProtection(entity), player.getUniqueId(), permissions);
    }

    @Override
    public boolean canAccess(final Protection protection, final Player player, final String... permissions) {
        return canAccess(protection, player.getUniqueId(), permissions);
    }

    @Override
    public boolean canAccess(final Protection protection, final UUID uuid, final String... permissions) {
        return canAccess(protection, new BukkitPlayerResolver(bolt, uuid), permissions);
    }

    @Override
    public boolean canAccess(final Protection protection, final SourceResolver sourceResolver, final String... permissions) {
        if (protection == null || permissions.length == 0) {
            return true;
        }
        return permissions.length == 1 ? canAccessSingle(protection, sourceResolver, permissions[0]) : canAccessMulti(protection, sourceResolver, permissions);
    }

    private boolean canAccessMulti(final Protection protection, final SourceResolver sourceResolver, final String... permissions) {
        final Set<String> unresolved = new HashSet<>(Arrays.asList(permissions));
        final Source ownerSource = Source.player(protection.getOwner());
        if (sourceResolver.resolve(ownerSource) || sourceResolver.resolve(ADMIN_PERMISSION_SOURCE)) {
            unresolved.removeAll(DefaultAccess.OWNER);
            if (unresolved.isEmpty()) {
                return true;
            }
        }
        if (sourceResolver.resolve(MOD_PERMISSION_SOURCE)) {
            unresolved.removeAll(DefaultAccess.DISPLAY);
            if (unresolved.isEmpty()) {
                return true;
            }
        }
        final AccessRegistry accessRegistry = bolt.getAccessRegistry();
        final Access protectionType = accessRegistry.getProtectionByType(protection.getType()).orElse(null);
        if (protectionType != null) {
            unresolved.removeAll(protectionType.permissions());
            if (unresolved.isEmpty()) {
                return true;
            }
        }
        for (final Map.Entry<String, String> entry : protection.getAccess().entrySet()) {
            if (sourceResolver.resolve(Source.parse(entry.getKey()))) {
                final Access accessType = accessRegistry.getAccessByType(entry.getValue()).orElse(null);
                if (accessType != null) {
                    unresolved.removeAll(accessType.permissions());
                    if (unresolved.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        final AccessList accessList = bolt.getStore().loadAccessList(protection.getOwner()).join();
        if (accessList != null) {
            for (final Map.Entry<String, String> entry : accessList.getAccess().entrySet()) {
                if (sourceResolver.resolve(Source.parse(entry.getKey()))) {
                    final Access accessType = accessRegistry.getAccessByType(entry.getValue()).orElse(null);
                    if (accessType != null) {
                        unresolved.removeAll(accessType.permissions());
                        if (unresolved.isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        unresolved.removeIf(permission -> sourceResolver.resolve(Source.of(SourceTypes.PERMISSION, "bolt.permission." + permission)));
        return unresolved.isEmpty();
    }

    private boolean canAccessSingle(final Protection protection, final SourceResolver sourceResolver, final String permission) {
        final Source ownerSource = Source.player(protection.getOwner());
        if (sourceResolver.resolve(ownerSource) || sourceResolver.resolve(ADMIN_PERMISSION_SOURCE)) {
            if (DefaultAccess.OWNER.contains(permission)) {
                return true;
            }
        }
        if (sourceResolver.resolve(MOD_PERMISSION_SOURCE)) {
            if (DefaultAccess.DISPLAY.contains(permission)) {
                return true;
            }
        }
        final AccessRegistry accessRegistry = bolt.getAccessRegistry();
        final Access protectionType = accessRegistry.getProtectionByType(protection.getType()).orElse(null);
        if (protectionType != null) {
            if (protectionType.permissions().contains(permission)) {
                return true;
            }
        }
        for (final Map.Entry<String, String> entry : protection.getAccess().entrySet()) {
            if (sourceResolver.resolve(Source.parse(entry.getKey()))) {
                final Access accessType = accessRegistry.getAccessByType(entry.getValue()).orElse(null);
                if (accessType != null) {
                    if (accessType.permissions().contains(permission)) {
                        return true;
                    }
                }
            }
        }
        final AccessList accessList = bolt.getStore().loadAccessList(protection.getOwner()).join();
        if (accessList != null) {
            for (final Map.Entry<String, String> entry : accessList.getAccess().entrySet()) {
                if (sourceResolver.resolve(Source.parse(entry.getKey()))) {
                    final Access accessType = accessRegistry.getAccessByType(entry.getValue()).orElse(null);
                    if (accessType != null) {
                        if (accessType.permissions().contains(permission)) {
                            return true;
                        }
                    }
                }
            }
        }
        return sourceResolver.resolve(Source.of(SourceTypes.PERMISSION, "bolt.permission." + permission));
    }

    @Override
    public void registerPlayerSourceResolver(PlayerSourceResolver playerSourceResolver) {
        bolt.getRegisteredPlayerResolvers().add(playerSourceResolver);
    }

    private Protection matchProtection(final Block block) {
        for (final BlockMatcher blockMatcher : enabledBlockMatchers) {
            if (blockMatcher.canMatch(block)) {
                final Match match = blockMatcher.findMatch(block);
                if (match != null) {
                    for (final Block matchBlock : match.blocks()) {
                        final BlockProtection protection = loadProtection(matchBlock);
                        if (protection != null) {
                            return protection;
                        }
                    }
                    for (final Entity matchEntity : match.entities()) {
                        final EntityProtection protection = loadProtection(matchEntity);
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
        for (final EntityMatcher entityMatcher : enabledEntityMatchers) {
            if (entityMatcher.canMatch(entity)) {
                final Match match = entityMatcher.findMatch(entity);
                if (match != null) {
                    for (final Block matchBlock : match.blocks()) {
                        final BlockProtection protection = loadProtection(matchBlock);
                        if (protection != null) {
                            return protection;
                        }
                    }
                    for (final Entity matchEntity : match.entities()) {
                        final EntityProtection protection = loadProtection(matchEntity);
                        if (protection != null) {
                            return protection;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void registerSourceTransformer(String sourceType, SourceTransformer sourceTransformer) {
        this.sourceTransformers.put(sourceType, sourceTransformer);
    }

    private void registerDefaultSourceTransformers() {
        this.sourceTransformers.put(SourceTypes.PLAYER, new PlayerSourceTransformer(this));
        this.sourceTransformers.put(SourceTypes.PASSWORD, new PasswordSourceTransformer());
        this.sourceTransformers.put(SourceTypes.GROUP, new GroupSourceTransformer(this));
    }

    public SourceTransformer getSourceTransformer(String type) {
        return this.sourceTransformers.getOrDefault(type, SourceTransformer.DEFAULT);
    }
}

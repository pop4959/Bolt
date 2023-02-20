package org.popcraft.bolt;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.command.impl.ConvertCommand;
import org.popcraft.bolt.command.impl.DebugCommand;
import org.popcraft.bolt.command.impl.EditCommand;
import org.popcraft.bolt.command.impl.InfoCommand;
import org.popcraft.bolt.command.impl.LockCommand;
import org.popcraft.bolt.command.impl.PasswordCommand;
import org.popcraft.bolt.command.impl.ModeCommand;
import org.popcraft.bolt.command.impl.ReportCommand;
import org.popcraft.bolt.command.impl.TransferCommand;
import org.popcraft.bolt.command.impl.UnlockCommand;
import org.popcraft.bolt.data.SQLiteStore;
import org.popcraft.bolt.data.SimpleProtectionCache;
import org.popcraft.bolt.data.SimpleProfileCache;
import org.popcraft.bolt.data.ProfileCache;
import org.popcraft.bolt.listeners.BlockListener;
import org.popcraft.bolt.listeners.EntityListener;
import org.popcraft.bolt.listeners.InventoryListener;
import org.popcraft.bolt.listeners.PlayerListener;
import org.popcraft.bolt.listeners.adapter.PlayerRecipeBookClickListener;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.matcher.block.AmethystClusterMatcher;
import org.popcraft.bolt.matcher.block.ArmorStandMatcher;
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
import org.popcraft.bolt.matcher.block.GlowLichenMatcher;
import org.popcraft.bolt.matcher.block.GrassMatcher;
import org.popcraft.bolt.matcher.block.HangingRootsMatcher;
import org.popcraft.bolt.matcher.block.HangingVineMatcher;
import org.popcraft.bolt.matcher.block.ItemFrameMatcher;
import org.popcraft.bolt.matcher.block.LadderMatcher;
import org.popcraft.bolt.matcher.block.LanternMatcher;
import org.popcraft.bolt.matcher.block.LeashHitchMatcher;
import org.popcraft.bolt.matcher.block.LilyPadMatcher;
import org.popcraft.bolt.matcher.block.MangrovePropaguleMatcher;
import org.popcraft.bolt.matcher.block.MossCarpetMatcher;
import org.popcraft.bolt.matcher.block.MushroomMatcher;
import org.popcraft.bolt.matcher.block.NetherWartMatcher;
import org.popcraft.bolt.matcher.block.PaintingMatcher;
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
import org.popcraft.bolt.matcher.block.SkulkVeinMatcher;
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
import org.popcraft.bolt.matcher.block.TrapdoorMatcher;
import org.popcraft.bolt.matcher.block.TripwireHookMatcher;
import org.popcraft.bolt.matcher.block.UprootMatcher;
import org.popcraft.bolt.matcher.block.VineMatcher;
import org.popcraft.bolt.matcher.entity.EntityMatcher;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Access;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.EnumUtil;
import org.popcraft.bolt.util.Permissible;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.lang.Translator;
import org.popcraft.bolt.util.Source;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BoltPlugin extends JavaPlugin {
    public static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("boltDebug", "false"));
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
            new DeadBushMatcher(), new HangingRootsMatcher(), new PointedDripstoneMatcher(), new FireMatcher(),
            new GlowLichenMatcher(), new LilyPadMatcher(), new RepeaterMatcher(), new SporeBlossomMatcher(),
            new SoulFireMatcher(), new FrogspawnMatcher(), new MangrovePropaguleMatcher(), new SkulkVeinMatcher());
    private static final List<EntityMatcher> ENTITY_MATCHERS = List.of();
    private final Map<String, BoltCommand> commands = new HashMap<>();
    private final Path profileCachePath = getDataFolder().toPath().resolve("profiles");
    private final ProfileCache profileCache = new SimpleProfileCache(profileCachePath);
    private final Map<Material, Access> protectableBlocks = new EnumMap<>(Material.class);
    private final Map<EntityType, Access> protectableEntities = new EnumMap<>(EntityType.class);
    private String defaultProtectionType = "private";
    private String defaultAccessType = "normal";
    private Bolt bolt;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.bolt = new Bolt(new SimpleProtectionCache(new SQLiteStore(getDataFolder().getPath())));
        Translator.load(getDataFolder().toPath(), getConfig().getString("language", "en"));
        registerAccessTypes();
        registerProtectableAccess();
        BoltComponents.enable(this);
        registerEvents();
        registerCommands();
        profileCache.load();
        final Metrics metrics = new Metrics(this, 17711);
        registerCustomCharts(metrics);
    }

    private void registerCustomCharts(final Metrics metrics) {
        metrics.addCustomChart(new SimplePie("config_language", () -> getConfig().getString("language", "en")));
        metrics.addCustomChart(new AdvancedPie("config_protections", () -> {
            final Map<String, Integer> map = new HashMap<>();
            bolt.getAccessRegistry().access().stream().filter(Access::protection).toList().forEach(access -> map.put(access.type(), 1));
            return map;
        }));
        metrics.addCustomChart(new AdvancedPie("config_access", () -> {
            final Map<String, Integer> map = new HashMap<>();
            bolt.getAccessRegistry().access().stream().filter(access -> !access.protection()).toList().forEach(access -> map.put(access.type(), 1));
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
        metrics.addCustomChart(new SimplePie("protections_blocks", () -> String.valueOf((bolt.getStore().loadBlockProtections().join().size() / 1000) * 1000)));
        metrics.addCustomChart(new SimplePie("protections_entities", () -> String.valueOf((bolt.getStore().loadEntityProtections().join().size() / 1000) * 1000)));
    }

    @Override
    public void onDisable() {
        BoltComponents.disable();
        HandlerList.unregisterAll(this);
        commands.clear();
        this.bolt.getStore().flush().join();
    }

    private void registerAccessTypes() {
        final ConfigurationSection protections = getConfig().getConfigurationSection("protections");
        if (protections != null) {
            for (final String type : protections.getKeys(false)) {
                final List<String> permissions = protections.getStringList(type);
                bolt.getAccessRegistry().register(true, type, new HashSet<>(permissions));
                if (defaultProtectionType == null || permissions.size() < bolt.getAccessRegistry().get(defaultProtectionType).map(Access::permissions).map(Set::size).orElse(0)) {
                    defaultProtectionType = type;
                }
            }
        }
        final ConfigurationSection access = getConfig().getConfigurationSection("access");
        if (access != null) {
            for (final String type : access.getKeys(false)) {
                final List<String> permissions = access.getStringList(type);
                bolt.getAccessRegistry().register(false, type, new HashSet<>(permissions));
                if (defaultAccessType == null || permissions.size() < bolt.getAccessRegistry().get(defaultAccessType).map(Access::permissions).map(Set::size).orElse(0)) {
                    defaultAccessType = type;
                }
            }
        }
    }

    private void registerProtectableAccess() {
        final ConfigurationSection blocks = getConfig().getConfigurationSection("blocks");
        if (blocks != null) {
            for (final String key : blocks.getKeys(false)) {
                final String autoProtectType = blocks.getString("%s.autoProtect".formatted(key), "false");
                final Access defaultAccess = bolt.getAccessRegistry().get(autoProtectType).orElse(null);
                if (key.startsWith("#")) {
                    final Tag<Material> tag = resolveTagProtectableAccess(Tag.REGISTRY_BLOCKS, Material.class, key.substring(1));
                    if (tag == null) {
                        getLogger().warning(() -> "Invalid block tag defined: %s. Skipping.".formatted(key));
                        continue;
                    }
                    tag.getValues().forEach(block -> protectableBlocks.put(block, defaultAccess));
                } else {
                    EnumUtil.valueOf(Material.class, key.toUpperCase()).filter(Material::isBlock).ifPresentOrElse(block -> protectableBlocks.put(block, defaultAccess), () -> getLogger().warning(() -> "Invalid block defined: %s. Skipping.".formatted(key)));
                }
            }
        }
        final ConfigurationSection entities = getConfig().getConfigurationSection("entities");
        if (entities != null) {
            for (final String key : entities.getKeys(false)) {
                final String autoProtectType = entities.getString("%s.autoProtect".formatted(key), "false");
                final Access defaultAccess = bolt.getAccessRegistry().get(autoProtectType).orElse(null);
                if (key.startsWith("#")) {
                    final Tag<EntityType> tag = resolveTagProtectableAccess(Tag.REGISTRY_ENTITY_TYPES, EntityType.class, key.substring(1));
                    if (tag == null) {
                        getLogger().warning(() -> "Invalid entity tag defined: %s. Skipping.".formatted(key));
                        continue;
                    }
                    tag.getValues().forEach(entity -> protectableEntities.put(entity, defaultAccess));
                } else {
                    EnumUtil.valueOf(EntityType.class, key.toUpperCase()).ifPresentOrElse(entity -> protectableEntities.put(entity, defaultAccess), () -> getLogger().warning(() -> "Invalid entity defined: %s. Skipping.".formatted(key)));
                }
            }
        }
    }

    private <T extends Keyed> Tag<T> resolveTagProtectableAccess(final String registry, final Class<T> clazz, final String name) {
        final NamespacedKey tagKey = NamespacedKey.fromString(name);
        return tagKey == null ? null : getServer().getTag(registry, tagKey, clazz);
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
        commands.put("convert", new ConvertCommand(this));
        commands.put("debug", new DebugCommand(this));
        commands.put("edit", new EditCommand(this));
        commands.put("info", new InfoCommand(this));
        commands.put("lock", new LockCommand(this));
        commands.put("mode", new ModeCommand(this));
        commands.put("password", new PasswordCommand(this));
        commands.put("report", new ReportCommand(this));
        commands.put("transfer", new TransferCommand(this));
        commands.put("unlock", new UnlockCommand(this));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        final boolean isBoltCommand = "bolt".equalsIgnoreCase(command.getName());
        final int commandStart = isBoltCommand ? 1 : 0;
        if (args.length < commandStart) {
            return true;
        }
        final String commandKey = (isBoltCommand ? args[0] : command.getName()).toLowerCase();
        if (!commands.containsKey(commandKey)) {
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
            commands.keySet().stream().filter(name -> sender.hasPermission(COMMAND_PERMISSION_KEY + name)).forEach(suggestions::add);
        } else if (commands.containsKey(commandKey) && sender.hasPermission(COMMAND_PERMISSION_KEY + commandKey)) {
            suggestions.addAll(commands.get(commandKey).suggestions(new Arguments(Arrays.copyOfRange(args, commandStart, args.length))));
        }
        return suggestions.stream()
                .filter(s -> s.toLowerCase().contains(args[args.length - 1].toLowerCase()))
                .toList();
    }

    public Bolt getBolt() {
        return bolt;
    }

    public ProfileCache getProfileCache() {
        return profileCache;
    }

    public BoltPlayer player(final Player player) {
        return player(player.getUniqueId());
    }

    public BoltPlayer player(final UUID uuid) {
        return bolt.getBoltPlayer(uuid);
    }

    public boolean isProtectable(final Block block) {
        return DEBUG || protectableBlocks.containsKey(block.getType());
    }

    public boolean isProtectable(final Entity entity) {
        return DEBUG || protectableEntities.containsKey(entity.getType());
    }

    public Access getDefaultAccess(final Block block) {
        return protectableBlocks.get(block.getType());
    }

    public Access getDefaultAccess(final Entity entity) {
        return protectableEntities.get(entity.getType());
    }

    public String getDefaultProtectionType() {
        return defaultProtectionType;
    }

    public String getDefaultAccessType() {
        return defaultAccessType;
    }

    public Optional<Protection> findProtection(final Block block) {
        final Protection protection = bolt.getStore().loadBlockProtection(BukkitAdapter.blockLocation(block)).join();
        return Optional.ofNullable(protection != null ? protection : matchProtection(block));
    }

    public Optional<Protection> findProtection(final Entity entity) {
        final Protection protection = bolt.getStore().loadEntityProtection(entity.getUniqueId()).join();
        return Optional.ofNullable(protection != null ? protection : matchProtection(entity));
    }

    public void saveProtection(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            bolt.getStore().saveBlockProtection(blockProtection);
        } else if (protection instanceof final EntityProtection entityProtection) {
            bolt.getStore().saveEntityProtection(entityProtection);
        }
    }

    public void removeProtection(final Protection protection) {
        if (protection instanceof final BlockProtection blockProtection) {
            bolt.getStore().removeBlockProtection(blockProtection);
        } else if (protection instanceof final EntityProtection entityProtection) {
            bolt.getStore().removeEntityProtection(entityProtection);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canAccess(final Block block, final Player player, final String... permissions) {
        return findProtection(block).map(protection -> canAccess(protection, player.getUniqueId(), permissions)).orElse(true);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canAccess(final Entity entity, final Player player, final String... permissions) {
        return findProtection(entity).map(protection -> canAccess(protection, player.getUniqueId(), permissions)).orElse(true);
    }

    public boolean canAccess(final Protection protection, final Player player, final String... permissions) {
        return canAccess(protection, player.getUniqueId(), permissions);
    }

    public boolean canAccess(final Protection protection, final UUID uuid, final String... permissions) {
        return canAccess(protection, bolt.getBoltPlayer(uuid), permissions);
    }

    public boolean canAccess(final Protection protection, final Permissible permissible, String... permissions) {
        final Set<String> sources = permissible.sources();
        final String ownerSource = Source.fromPlayer(protection.getOwner());
        if (sources.contains(ownerSource)) {
            return true;
        }
        if (permissible instanceof BoltPlayer boltPlayer) {
            final Player permissiblePlayer = getServer().getPlayer(boltPlayer.getUuid());
            if (permissiblePlayer != null && permissiblePlayer.hasPermission("bolt.admin")) {
                return true;
            }
        }
        final AccessRegistry accessRegistry = bolt.getAccessRegistry();
        final Set<String> heldPermissions = new HashSet<>();
        accessRegistry.get(protection.getType()).ifPresent(access -> heldPermissions.addAll(access.permissions()));
        protection.getAccess().forEach((source, accessType) -> {
            if (sources.contains(source)) {
                accessRegistry.get(accessType).ifPresent(access -> heldPermissions.addAll(access.permissions()));
            }
        });
        for (final String permission : permissions) {
            if (!heldPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    private Protection matchProtection(final Block block) {
        for (final BlockMatcher blockMatcher : BLOCK_MATCHERS) {
            if (blockMatcher.canMatch(block)) {
                final Optional<Match> optionalMatch = blockMatcher.findMatch(block);
                if (optionalMatch.isPresent()) {
                    final Match match = optionalMatch.get();
                    for (final Block matchBlock : match.blocks()) {
                        final BlockProtection protection = bolt.getStore().loadBlockProtection(BukkitAdapter.blockLocation(matchBlock)).join();
                        if (protection != null) {
                            return protection;
                        }
                    }
                    for (final Entity matchEntity : match.entities()) {
                        final EntityProtection protection = bolt.getStore().loadEntityProtection(matchEntity.getUniqueId()).join();
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
                        final BlockProtection protection = bolt.getStore().loadBlockProtection(BukkitAdapter.blockLocation(matchBlock)).join();
                        if (protection != null) {
                            return protection;
                        }
                    }
                    for (final Entity matchEntity : match.entities()) {
                        final EntityProtection protection = bolt.getStore().loadEntityProtection(matchEntity.getUniqueId()).join();
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

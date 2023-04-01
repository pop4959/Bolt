package org.popcraft.bolt.data.migration.lwc;

import com.google.gson.Gson;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.MemoryStore;
import org.popcraft.bolt.data.SQLStore;
import org.popcraft.bolt.data.sql.Statements;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceType;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.nbt.CompoundTag;
import org.popcraft.chunky.nbt.IntArrayTag;
import org.popcraft.chunky.nbt.ListTag;
import org.popcraft.chunky.nbt.StringTag;
import org.popcraft.chunky.world.RegionFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LWCMigration {
    private static final Block BLOCK_AIR = new Block(-1, "AIR");
    private static final String DEFAULT_PROTECTION_PUBLIC = "public";
    private static final String DEFAULT_PROTECTION_DEPOSIT = "deposit";
    private static final String DEFAULT_PROTECTION_DISPLAY = "display";
    private static final String DEFAULT_PROTECTION_PRIVATE = "private";
    private static final String DEFAULT_ACCESS_NORMAL = "normal";
    private static final String DEFAULT_ACCESS_ADMIN = "admin";
    private static final Map<String, EntityType> ENTITY_TYPE_KEYS = Arrays.stream(EntityType.values())
            .filter(entityType -> !EntityType.UNKNOWN.equals(entityType))
            .collect(Collectors.toMap(entityType -> entityType.getKey().toString(), entityType -> entityType));
    private final BoltPlugin plugin;
    private final Map<Integer, BlockProtection> entityBlocks = new ConcurrentHashMap<>();

    public LWCMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<MemoryStore> convertAsync() {
        return CompletableFuture.supplyAsync(this::convert);
    }

    private MemoryStore convert() {
        final MemoryStore store = new MemoryStore();
        final Map<Integer, Block> blocks = new HashMap<>();
        final List<Protection> protections = new ArrayList<>();
        final FileConfiguration lwcCoreConfig = YamlConfiguration.loadConfiguration(plugin.getPluginsPath().resolve("LWC/core.yml").toFile());
        final SQLStore.Configuration configuration = new SQLStore.Configuration(
                lwcCoreConfig.getString("database.adapter", "sqlite"),
                lwcCoreConfig.getString("database.path", "%s/LWC/lwc.db".formatted(plugin.getPluginsPath().toFile().getName())),
                lwcCoreConfig.getString("database.host", ""),
                lwcCoreConfig.getString("database.database", ""),
                lwcCoreConfig.getString("database.username", ""),
                lwcCoreConfig.getString("database.password", ""),
                lwcCoreConfig.getString("database.prefix", "lwc_"),
                Map.of("useSSL", lwcCoreConfig.getString("database.useSSL", "false"))
        );
        final String connectionUrl = "mysql".equals(configuration.type()) ?
                "jdbc:mysql://%s/%s".formatted(configuration.hostname(), configuration.database()) :
                "jdbc:sqlite:%s".formatted(configuration.path());
        try (final Connection connection = DriverManager.getConnection(connectionUrl, configuration.username(), configuration.password());
             final Statement statement = connection.createStatement()) {
            final ResultSet blockSet = statement.executeQuery(Statements.LWC_SELECT_ALL_BLOCK_IDS.get(configuration.type()).formatted(configuration.prefix()));
            while (blockSet.next()) {
                final int id = blockSet.getInt("id");
                final String name = blockSet.getString("name");
                blocks.put(id, new Block(id, name));
            }
            final ResultSet protectionSet = statement.executeQuery(Statements.LWC_SELECT_ALL_PROTECTIONS.get(configuration.type()).formatted(configuration.prefix()));
            while (protectionSet.next()) {
                protections.add(new Protection(
                        protectionSet.getInt("id"),
                        protectionSet.getString("owner"),
                        protectionSet.getInt("type"),
                        protectionSet.getInt("x"),
                        protectionSet.getInt("y"),
                        protectionSet.getInt("z"),
                        protectionSet.getString("data"),
                        protectionSet.getInt("blockId"),
                        protectionSet.getString("world"),
                        protectionSet.getString("password"),
                        protectionSet.getDate("date"),
                        protectionSet.getLong("last_accessed")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return store;
        }
        final Gson gson = new Gson();
        for (final Protection protection : protections) {
            final String protectionType;
            if (protection.type() == ProtectionType.PUBLIC.ordinal()) {
                protectionType = DEFAULT_PROTECTION_PUBLIC;
            } else if (protection.type() == ProtectionType.DONATION.ordinal()) {
                protectionType = DEFAULT_PROTECTION_DEPOSIT;
            } else if (protection.type() == ProtectionType.DISPLAY.ordinal()) {
                protectionType = DEFAULT_PROTECTION_DISPLAY;
            } else {
                protectionType = DEFAULT_PROTECTION_PRIVATE;
            }
            final Map<String, String> access = new HashMap<>();
            final Data data = gson.fromJson(protection.data(), Data.class);
            if (data != null) {
                final List<DataFlag> dataFlags = data.getFlags();
                if (dataFlags != null) {
                    for (DataFlag flag : dataFlags) {
                        if (flag.getId() == ProtectionFlag.REDSTONE.ordinal()) {
                            access.put(Source.of(SourceType.REDSTONE).toString(), DEFAULT_ACCESS_ADMIN);
                        } else if (flag.getId() == ProtectionFlag.HOPPER.ordinal()) {
                            access.put(Source.of(SourceType.BLOCK).toString(), DEFAULT_ACCESS_ADMIN);
                        }
                    }
                }
                final List<DataRights> dataRights = data.getRights();
                if (dataRights != null) {
                    for (DataRights rights : data.getRights()) {
                        final String accessType = rights.getRights() == Permission.Access.ADMIN.ordinal() ? DEFAULT_ACCESS_ADMIN : DEFAULT_ACCESS_NORMAL;
                        if (rights.getType() == Permission.Type.GROUP.ordinal()) {
                            access.put(Source.of(SourceType.GROUP, rights.getName()).toString(), accessType);
                        } else if (rights.getType() == Permission.Type.PLAYER.ordinal()) {
                            final UUID uuid = Optional.ofNullable(BukkitAdapter.findProfileByName(rights.getName()).uuid())
                                    .orElseGet(() -> BukkitAdapter.lookupProfileByName(rights.getName()).join().uuid());
                            if (uuid != null) {
                                access.put(Source.player(uuid).toString(), accessType);
                            }
                        } else if (rights.getType() == Permission.Type.TOWN.ordinal()) {
                            access.put(Source.of(SourceType.TOWN, rights.getName()).toString(), accessType);
                        } else if (rights.getType() == Permission.Type.REGION.ordinal()) {
                            access.put(Source.of(SourceType.REGION, rights.getName()).toString(), accessType);
                        }
                    }
                }
            }
            if (protection.password() != null && !protection.password().isEmpty()) {
                final Source passwordSource = Source.password(protection.password());
                if (passwordSource != null) {
                    access.put(passwordSource.toString(), DEFAULT_ACCESS_NORMAL);
                }
            }
            final UUID ownerUuid = Optional.ofNullable(BukkitAdapter.findProfileByName(protection.owner()).uuid())
                    .orElseGet(() -> BukkitAdapter.lookupProfileByName(protection.owner()).join().uuid());
            final BlockProtection blockProtection = new BlockProtection(
                    UUID.randomUUID(),
                    Objects.requireNonNullElse(ownerUuid, BukkitAdapter.NIL_UUID),
                    protectionType,
                    protection.date().getTime(),
                    TimeUnit.MILLISECONDS.convert(protection.lastAccessed(), TimeUnit.SECONDS),
                    access,
                    protection.world(),
                    protection.x(),
                    protection.y(),
                    protection.z(),
                    blocks.getOrDefault(protection.blockId(), BLOCK_AIR).name()
            );
            if (EntityBlock.check(blockProtection)) {
                entityBlocks.put(blockProtection.getX(), blockProtection);
            } else {
                store.saveBlockProtection(blockProtection);
            }
        }
        return store;
    }

    public boolean hasEntityBlocks() {
        return entityBlocks.size() > 0;
    }

    public CompletableFuture<MemoryStore> convertEntityBlocks() {
        return CompletableFuture.supplyAsync(() -> {
            final MemoryStore store = new MemoryStore();
            ChunkyProvider.get().getServer().getWorlds().forEach(world -> {
                final Optional<Path> entityDirectory = world.getEntitiesDirectory();
                if (entityDirectory.isPresent()) {
                    try (final Stream<Path> regionWalker = Files.walk(entityDirectory.get())) {
                        regionWalker.filter(path -> {
                            final String fileName = path.getFileName().toString();
                            return fileName.startsWith("r.") && fileName.endsWith(".mca");
                        }).forEach(region -> {
                            final RegionFile regionFile = new RegionFile(region.toFile());
                            final List<CompoundTag> entities = new ArrayList<>();
                            regionFile.getChunks().forEach(chunk -> chunk.getData().getList("Entities")
                                    .map(ListTag::value)
                                    .stream()
                                    .flatMap(Collection::stream)
                                    .filter(CompoundTag.class::isInstance)
                                    .map(CompoundTag.class::cast)
                                    .forEach(entities::add)
                            );
                            for (final CompoundTag entity : entities) {
                                final UUID uuid = entity.getIntArray("UUID")
                                        .map(IntArrayTag::value)
                                        .filter(array -> array.length == 4)
                                        .map(array -> {
                                            final long mostSigBitsHigh = array[0] & 0xFFFFFFFFL;
                                            final long mostSigBitsLow = array[1] & 0xFFFFFFFFL;
                                            final long mostSigBits = (mostSigBitsHigh << 32) | mostSigBitsLow;
                                            final long leastSigBitsHigh = array[2] & 0xFFFFFFFFL;
                                            final long leastSigBitsLow = array[3] & 0xFFFFFFFFL;
                                            final long leastSigBits = (leastSigBitsHigh << 32) | leastSigBitsLow;
                                            return new UUID(mostSigBits, leastSigBits);
                                        })
                                        .orElse(null);
                                if (uuid == null) {
                                    continue;
                                }
                                final int magic = EntityBlock.magic(uuid);
                                final BlockProtection blockProtection = entityBlocks.get(magic);
                                if (blockProtection == null) {
                                    continue;
                                }
                                final EntityType entityType = entity.getString("id")
                                        .map(StringTag::value)
                                        .map(ENTITY_TYPE_KEYS::get)
                                        .orElse(EntityType.UNKNOWN);
                                final EntityProtection entityProtection = new EntityProtection(
                                        uuid,
                                        blockProtection.getOwner(),
                                        blockProtection.getType(),
                                        blockProtection.getCreated(),
                                        blockProtection.getAccessed(),
                                        blockProtection.getAccess(),
                                        entityType.name()
                                );
                                store.saveEntityProtection(entityProtection);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return store;
        });
    }
}

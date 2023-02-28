package org.popcraft.bolt.data.migration.lwc;

import com.google.gson.Gson;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.SQLStore;
import org.popcraft.bolt.data.sql.Statements;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.util.SourceType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BoltMigration {
    private final BoltPlugin plugin;

    public BoltMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> convertAsync() {
        return CompletableFuture.runAsync(this::convert);
    }

    private void convert() {
        final Map<String, Integer> blockIds = new HashMap<>();
        final AtomicInteger blockId = new AtomicInteger();
        final Set<BlockLocation> existing = new HashSet<>();
        final FileConfiguration lwcCoreConfig = YamlConfiguration.loadConfiguration(plugin.getDataFolder().toPath().resolve("../LWC/core.yml").toFile());
        final SQLStore.Configuration configuration = new SQLStore.Configuration(
                lwcCoreConfig.getString("database.adapter", "sqlite"),
                lwcCoreConfig.getString("database.path", "plugins/LWC/lwc.db"),
                lwcCoreConfig.getString("database.host", ""),
                lwcCoreConfig.getString("database.database", ""),
                lwcCoreConfig.getString("database.username", ""),
                lwcCoreConfig.getString("database.password", ""),
                lwcCoreConfig.getString("database.prefix", "lwc_"),
                List.of("useSSL", lwcCoreConfig.getString("database.useSSL", "false"))
        );
        final String connectionUrl = "mysql".equals(configuration.type()) ?
                "jdbc:mysql://%s:%s@%s/%s".formatted(configuration.username(), configuration.password(), configuration.hostname(), configuration.database()) :
                "jdbc:sqlite:%s".formatted(configuration.path());
        try (final Connection connection = DriverManager.getConnection(connectionUrl);
             final Statement statement = connection.createStatement();
             final PreparedStatement addBlock = connection.prepareStatement(Statements.LWC_INSERT_BLOCK_ID.get(configuration.type()).formatted(configuration.prefix()));
             final PreparedStatement addProtection = connection.prepareStatement(Statements.LWC_INSERT_OR_IGNORE_PROTECTION.get(configuration.type()).formatted(configuration.prefix()))) {
            final ResultSet blockSet = statement.executeQuery(Statements.LWC_SELECT_ALL_BLOCK_IDS.get(configuration.type()).formatted(configuration.prefix()));
            while (blockSet.next()) {
                final int id = blockSet.getInt("id");
                final String name = blockSet.getString("name");
                blockIds.put(name, id);
                if (id > blockId.get()) {
                    blockId.set(id);
                }
            }
            final ResultSet existingSet = statement.executeQuery(Statements.LWC_SELECT_ALL_PROTECTIONS.get(configuration.type()).formatted(configuration.prefix()));
            while (existingSet.next()) {
                existing.add(new BlockLocation(
                        existingSet.getString("world"),
                        existingSet.getInt("x"),
                        existingSet.getInt("y"),
                        existingSet.getInt("z")
                ));
            }
            final Gson gson = new Gson();
            connection.setAutoCommit(false);
            for (final BlockProtection blockProtection : plugin.getBolt().getStore().loadBlockProtections().join()) {
                final String protectionBlock = blockProtection.getBlock();
                if (existing.contains(BukkitAdapter.blockLocation(blockProtection))) {
                    continue;
                }
                if (!blockIds.containsKey(protectionBlock)) {
                    final int nextId = blockId.incrementAndGet();
                    blockIds.put(protectionBlock, nextId);
                    addBlock.setInt(1, nextId);
                    addBlock.setString(2, protectionBlock);
                    addBlock.execute();
                }
                addProtection.setString(1, blockProtection.getOwner().toString());
                addProtection.setInt(2, convertProtectionType(blockProtection));
                addProtection.setInt(3, blockProtection.getX());
                addProtection.setInt(4, blockProtection.getY());
                addProtection.setInt(5, blockProtection.getZ());
                addProtection.setString(6, gson.toJson(convertData(blockProtection)));
                addProtection.setInt(7, blockIds.get(protectionBlock));
                addProtection.setString(8, blockProtection.getWorld());
                addProtection.setString(9, convertPassword(blockProtection));
                addProtection.setString(10, new Timestamp(blockProtection.getCreated()).toString());
                addProtection.setLong(11, TimeUnit.SECONDS.convert(blockProtection.getAccessed(), TimeUnit.MILLISECONDS));
                addProtection.setString(12, null);
                addProtection.execute();
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int convertProtectionType(final BlockProtection blockProtection) {
        if ("public".equals(blockProtection.getType())) {
            return ProtectionType.PUBLIC.ordinal();
        } else if ("deposit".equals(blockProtection.getType())) {
            return ProtectionType.DONATION.ordinal();
        } else if ("display".equals(blockProtection.getType())) {
            return ProtectionType.DISPLAY.ordinal();
        }
        final boolean password = blockProtection.getAccess().entrySet().stream()
                .anyMatch(entry -> SourceType.PASSWORD.equals(Source.parse(entry.getKey()).getType()));
        if (password) {
            return ProtectionType.PASSWORD.ordinal();
        } else {
            return ProtectionType.PRIVATE.ordinal();
        }
    }

    private Data convertData(final BlockProtection blockProtection) {
        final Data data = new Data();
        final List<DataFlag> flags = new ArrayList<>();
        final List<DataRights> rights = new ArrayList<>();
        if (blockProtection.getAccess().isEmpty()) {
            data.setFlags(flags);
            data.setRights(rights);
            return data;
        }
        blockProtection.getAccess().forEach((entry, access) -> {
            final Source source = Source.parse(entry);
            if (SourceType.BLOCK.equals(source.getType())) {
                final DataFlag dataFlag = new DataFlag();
                dataFlag.setId(ProtectionFlag.HOPPER.ordinal());
                flags.add(dataFlag);
            }
            final Permission.Access permissionAccess = switch (access) {
                case "normal" -> Permission.Access.PLAYER;
                case "admin" -> Permission.Access.ADMIN;
                default -> null;
            };
            final Permission.Type permissionType = switch (source.getType()) {
                case SourceType.GROUP -> Permission.Type.GROUP;
                case SourceType.PLAYER -> Permission.Type.PLAYER;
                case SourceType.TOWN -> Permission.Type.TOWN;
                case SourceType.REGION -> Permission.Type.REGION;
                default -> null;
            };
            if (permissionAccess != null && permissionType != null) {
                final DataRights dataRights = new DataRights();
                dataRights.setRights(permissionAccess.ordinal());
                dataRights.setType(permissionType.ordinal());
                dataRights.setName(source.getIdentifier());
                rights.add(dataRights);
            }
        });
        data.setFlags(flags);
        data.setRights(rights);
        return data;
    }

    private String convertPassword(final BlockProtection blockProtection) {
        if (blockProtection.getAccess().isEmpty()) {
            return "";
        }
        return blockProtection.getAccess().keySet().stream()
                .map(Source::parse)
                .filter(source -> SourceType.PASSWORD.equals(source.getType()))
                .map(Source::getIdentifier)
                .findFirst()
                .orElse("");
    }
}

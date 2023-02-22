package org.popcraft.bolt.data.migration.lwc;

import com.google.gson.Gson;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.MemoryStore;
import org.popcraft.bolt.data.SQLStore;
import org.popcraft.bolt.data.sql.Statements;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Source;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LWCMigration {
    private static final Block BLOCK_AIR = new Block(-1, "AIR");
    private static final String DEFAULT_PROTECTION_PUBLIC = "public";
    private static final String DEFAULT_PROTECTION_DEPOSIT = "deposit";
    private static final String DEFAULT_PROTECTION_DISPLAY = "display";
    private static final String DEFAULT_PROTECTION_PRIVATE = "private";
    private static final String DEFAULT_ACCESS_NORMAL = "normal";
    private static final String DEFAULT_ACCESS_ADMIN = "admin";
    private final BoltPlugin plugin;

    public LWCMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public MemoryStore convert() {
        final MemoryStore store = new MemoryStore();
        final Map<Integer, Block> blocks = new HashMap<>();
        final List<Protection> protections = new ArrayList<>();
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
                for (DataFlag flag : data.getFlags()) {
                    if (flag.getId() == ProtectionFlag.REDSTONE.ordinal()) {
                        access.put(Source.from(Source.REDSTONE, Source.REDSTONE), DEFAULT_ACCESS_ADMIN);
                    } else if (flag.getId() == ProtectionFlag.HOPPER.ordinal()) {
                        access.put(Source.from(Source.BLOCK, Source.BLOCK), DEFAULT_ACCESS_ADMIN);
                    }
                }
                for (DataRights rights : data.getRights()) {
                    final String accessType = rights.getRights() == Permission.Access.ADMIN.ordinal() ? DEFAULT_ACCESS_ADMIN : DEFAULT_ACCESS_NORMAL;
                    if (rights.getType() == Permission.Type.GROUP.ordinal()) {
                        access.put(Source.from(Source.GROUP, rights.getName()), accessType);
                    } else if (rights.getType() == Permission.Type.PLAYER.ordinal()) {
                        access.put(Source.from(Source.PLAYER, rights.getName()), accessType);
                    } else if (rights.getType() == Permission.Type.TOWN.ordinal()) {
                        access.put(Source.from(Source.TOWN, rights.getName()), accessType);
                    } else if (rights.getType() == Permission.Type.REGION.ordinal()) {
                        access.put(Source.from(Source.REGION, rights.getName()), accessType);
                    }
                }
            }
            if (protection.password() != null && !protection.password().isEmpty()) {
                access.put(Source.from(Source.PASSWORD, protection.password()), DEFAULT_ACCESS_NORMAL);
            }
            final UUID ownerUUID = BukkitAdapter.findPlayerUniqueId(protection.owner());
            store.saveBlockProtection(new BlockProtection(
                    UUID.randomUUID(),
                    ownerUUID,
                    protectionType,
                    protection.date().getTime(),
                    TimeUnit.MILLISECONDS.convert(protection.lastAccessed(), TimeUnit.SECONDS),
                    access,
                    protection.world(),
                    protection.x(),
                    protection.y(),
                    protection.z(),
                    blocks.getOrDefault(protection.blockId(), BLOCK_AIR).name()
            ));
        }
        return store;
    }
}

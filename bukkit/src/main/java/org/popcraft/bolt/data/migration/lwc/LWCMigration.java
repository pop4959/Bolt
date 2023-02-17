package org.popcraft.bolt.data.migration.lwc;

import com.google.gson.Gson;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.MemoryStore;
import org.popcraft.bolt.data.Migration;
import org.popcraft.bolt.protection.BlockProtection;
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

public class LWCMigration implements Migration {
    private static final int PROTECTION_TYPE_PUBLIC = 0;
    private static final int PROTECTION_TYPE_DONATION = 5;
    private static final int PROTECTION_TYPE_DISPLAY = 6;
    private static final int FLAG_TYPE_REDSTONE = 0;
    private static final int FLAG_TYPE_HOPPER = 5;
    private static final int ACCESS_TYPE_GROUP = 0;
    private static final int ACCESS_TYPE_PLAYER = 1;
    private static final int ACCESS_TYPE_TOWN = 3;
    private static final int ACCESS_TYPE_REGION = 5;
    private static final int RIGHTS_TYPE_ADMIN = 2;
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

    @Override
    public MemoryStore convert() {
        final MemoryStore store = new MemoryStore();
        final Map<Integer, Block> blocks = new HashMap<>();
        final List<Protection> protections = new ArrayList<>();
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:lwc.db");
            try (Statement statement = connection.createStatement()) {
                try {
                    ResultSet blockSet = statement.executeQuery("select * from lwc_blocks");
                    while (blockSet.next()) {
                        final int id = blockSet.getInt("id");
                        final String name = blockSet.getString("name");
                        blocks.put(id, new Block(id, name));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                try {
                    ResultSet protectionSet = statement.executeQuery("select * from lwc_protections");
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
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        final Gson gson = new Gson();
        for (final Protection protection : protections) {
            final String protectionType;
            if (protection.type() == PROTECTION_TYPE_PUBLIC) {
                protectionType = DEFAULT_PROTECTION_PUBLIC;
            } else if (protection.type() == PROTECTION_TYPE_DONATION) {
                protectionType = DEFAULT_PROTECTION_DEPOSIT;
            } else if (protection.type() == PROTECTION_TYPE_DISPLAY) {
                protectionType = DEFAULT_PROTECTION_DISPLAY;
            } else {
                protectionType = DEFAULT_PROTECTION_PRIVATE;
            }
            final Map<String, String> access = new HashMap<>();
            final Data data = gson.fromJson(protection.data(), Data.class);
            if (data != null) {
                for (DataFlag flag : data.getFlags()) {
                    if (flag.getId() == FLAG_TYPE_REDSTONE) {
                        access.put(Source.from(Source.REDSTONE, Source.REDSTONE), DEFAULT_ACCESS_ADMIN);
                    } else if (flag.getId() == FLAG_TYPE_HOPPER) {
                        access.put(Source.from(Source.BLOCK, Source.BLOCK), DEFAULT_ACCESS_ADMIN);
                    }
                }
                for (DataRights rights : data.getRights()) {
                    final String accessType = rights.getRights() == RIGHTS_TYPE_ADMIN ? DEFAULT_ACCESS_ADMIN : DEFAULT_ACCESS_NORMAL;
                    if (rights.getType() == ACCESS_TYPE_GROUP) {
                        access.put(Source.from(Source.PERMISSION, rights.getName()), accessType);
                    } else if (rights.getType() == ACCESS_TYPE_PLAYER) {
                        access.put(Source.from(Source.PLAYER, rights.getName()), accessType);
                    } else if (rights.getType() == ACCESS_TYPE_TOWN) {
                        access.put(Source.from(Source.TOWN, rights.getName()), accessType);
                    } else if (rights.getType() == ACCESS_TYPE_REGION) {
                        access.put(Source.from(Source.REGION, rights.getName()), accessType);
                    }
                }
            }
            if (protection.password() != null && !protection.password().isEmpty()) {
                access.put(Source.from(Source.PASSWORD, protection.password()), DEFAULT_ACCESS_NORMAL);
            }
            UUID ownerUUID;
            try {
                ownerUUID = UUID.fromString(protection.owner());
            } catch (IllegalArgumentException e) {
                ownerUUID = plugin.getProfileCache().getUniqueId(protection.owner());
            }
            store.saveBlockProtection(new BlockProtection(
                    UUID.randomUUID(),
                    ownerUUID,
                    protectionType,
                    protection.date().getTime(),
                    protection.lastAccessed(),
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

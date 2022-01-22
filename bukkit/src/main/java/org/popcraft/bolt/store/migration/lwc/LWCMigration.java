package org.popcraft.bolt.store.migration.lwc;

import com.google.gson.Gson;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.store.MemoryStore;
import org.popcraft.bolt.store.migration.Migration;
import org.popcraft.bolt.util.Access;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.util.defaults.DefaultAccess;
import org.popcraft.bolt.util.defaults.DefaultProtectionType;
import org.popcraft.bolt.util.defaults.DefaultSource;

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
    private static final String LWC_SOURCE_TOWN = "town";
    private static final String LWC_SOURCE_REGION = "region";
    private static final Block BLOCK_AIR = new Block(-1, "AIR");

    @Override
    public MemoryStore convert() {
        final MemoryStore store = new MemoryStore();
        final Map<Integer, Block> blocks = new HashMap<>();
        final List<Protection> protections = new ArrayList<>();
        Connection connection = null;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:lwc.db");
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);  // set timeout to 30 sec.
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
                                protectionSet.getString("date"),
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
        for (final DefaultProtectionType defaultProtectionType : DefaultProtectionType.values()) {
            final Access access = new Access(defaultProtectionType.type(), defaultProtectionType.permissions());
            store.saveAccess(access);
        }
        for (final DefaultAccess defaultAccess : DefaultAccess.values()) {
            final Access access = new Access(defaultAccess.type(), defaultAccess.permissions());
            store.saveAccess(access);
        }
        final Gson gson = new Gson();
        for (final Protection protection : protections) {
            final String protectionType;
            if (protection.type() == PROTECTION_TYPE_PUBLIC) {
                protectionType = DefaultProtectionType.PUBLIC.type();
            } else if (protection.type() == PROTECTION_TYPE_DONATION) {
                protectionType = DefaultProtectionType.DEPOSIT.type();
            } else if (protection.type() == PROTECTION_TYPE_DISPLAY) {
                protectionType = DefaultProtectionType.DISPLAY.type();
            } else {
                protectionType = DefaultProtectionType.PRIVATE.type();
            }
            final Map<Source, String> access = new HashMap<>();
            final Data data = gson.fromJson(protection.data(), Data.class);
            if (data != null) {
                for (DataFlag flag : data.getFlags()) {
                    if (flag.getId() == FLAG_TYPE_REDSTONE) {
                        access.put(new Source(DefaultSource.REDSTONE.getType(), DefaultSource.REDSTONE.getType()), DefaultAccess.FULL.type());
                    } else if (flag.getId() == FLAG_TYPE_HOPPER) {
                        access.put(new Source(DefaultSource.HOPPER.getType(), DefaultSource.HOPPER.getType()), DefaultAccess.FULL.type());
                    }
                }
                for (DataRights rights : data.getRights()) {
                    final String accessType = rights.getRights() == RIGHTS_TYPE_ADMIN ? DefaultAccess.FULL.type() : DefaultAccess.BASIC.type();
                    if (rights.getType() == ACCESS_TYPE_GROUP) {
                        access.put(new Source(DefaultSource.PERMISSION.getType(), rights.getName()), accessType);
                    } else if (rights.getType() == ACCESS_TYPE_PLAYER) {
                        access.put(new Source(DefaultSource.PLAYER.getType(), rights.getName()), accessType);
                    } else if (rights.getType() == ACCESS_TYPE_TOWN) {
                        access.put(new Source(LWC_SOURCE_TOWN, rights.getName()), accessType);
                    } else if (rights.getType() == ACCESS_TYPE_REGION) {
                        access.put(new Source(LWC_SOURCE_REGION, rights.getName()), accessType);
                    }
                }
            }
            if (protection.password() != null && !protection.password().isEmpty()) {
                access.put(new Source(DefaultSource.PASSWORD.getType(), protection.password()), DefaultAccess.BASIC.type());
            }
            UUID ownerUUID;
            try {
                ownerUUID = UUID.fromString(protection.owner());
            } catch (IllegalArgumentException e) {
                ownerUUID = BukkitAdapter.playerUUID(protection.owner());
            }
            store.saveBlockProtection(new BlockProtection(
                    UUID.randomUUID(),
                    ownerUUID,
                    protectionType,
                    access,
                    blocks.getOrDefault(protection.blockId(), BLOCK_AIR).name(),
                    protection.world(),
                    protection.x(),
                    protection.y(),
                    protection.z()
            ));
        }
        return store;
    }
}

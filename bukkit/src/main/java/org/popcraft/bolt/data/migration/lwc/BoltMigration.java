package org.popcraft.bolt.data.migration.lwc;

import com.google.gson.Gson;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Source;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BoltMigration {
    private final BoltPlugin plugin;

    public BoltMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public void convert() {
        final Map<String, Integer> blockIds = new HashMap<>();
        final AtomicInteger blockId = new AtomicInteger();
        final Set<BlockLocation> existing = new HashSet<>();
        try (final Connection connection = DriverManager.getConnection("jdbc:sqlite:%s/lwc.db".formatted(plugin.getDataFolder().toPath().resolve("../LWC").toFile().getPath()));
             final Statement statement = connection.createStatement();
             final PreparedStatement addBlock = connection.prepareStatement("INSERT INTO lwc_blocks VALUES (?, ?);");
             final PreparedStatement addProtection = connection.prepareStatement("INSERT OR IGNORE INTO lwc_protections (owner, type, x, y, z, data, blockId, world, password, date, last_accessed, rights) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
            final ResultSet blockSet = statement.executeQuery("SELECT * FROM lwc_blocks");
            while (blockSet.next()) {
                final int id = blockSet.getInt("id");
                final String name = blockSet.getString("name");
                blockIds.put(name, id);
                if (id > blockId.get()) {
                    blockId.set(id);
                }
            }
            final ResultSet existingSet = statement.executeQuery("SELECT * FROM lwc_protections");
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
                .anyMatch(entry -> Source.PASSWORD.equals(Source.type(entry.getKey())));
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
        blockProtection.getAccess().forEach((source, access) -> {
            final String sourceType = Source.type(source);
            final String sourceIdentifier = Source.identifier(source);
            if (Source.BLOCK.equals(sourceType)) {
                final DataFlag dataFlag = new DataFlag();
                dataFlag.setId(ProtectionFlag.HOPPER.ordinal());
                flags.add(dataFlag);
            }
            final Permission.Access permissionAccess = switch (access) {
                case "normal" -> Permission.Access.PLAYER;
                case "admin" -> Permission.Access.ADMIN;
                default -> null;
            };
            final Permission.Type permissionType = switch (sourceType) {
                case Source.GROUP -> Permission.Type.GROUP;
                case Source.PLAYER -> Permission.Type.PLAYER;
                case Source.TOWN -> Permission.Type.TOWN;
                case Source.REGION -> Permission.Type.REGION;
                default -> null;
            };
            if (permissionAccess != null && permissionType != null) {
                final DataRights dataRights = new DataRights();
                dataRights.setRights(permissionAccess.ordinal());
                dataRights.setType(permissionType.ordinal());
                dataRights.setName(sourceIdentifier);
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
                .filter(source -> Source.PASSWORD.equals(Source.type(source)))
                .map(Source::identifier)
                .findFirst()
                .orElse("");
    }
}

package org.popcraft.bolt.data.store;

import org.popcraft.bolt.data.Access;
import org.popcraft.bolt.data.Source;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.protection.EntityProtection;
import org.popcraft.bolt.data.util.BlockLocation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SQLiteStore implements Store {
    private static final String JDBC_SQLITE_URL = "jdbc:sqlite:bolt.db";

    public SQLiteStore() {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS blocks (id varchar(36) PRIMARY KEY, owner varchar(36), type varchar(128), accesslist text, block varchar(128), world varchar(128), x integer, y integer, z integer);").execute();
            connection.prepareStatement("CREATE INDEX IF NOT EXISTS block_owner ON blocks(owner);").execute();
            connection.prepareStatement("CREATE UNIQUE INDEX IF NOT EXISTS block_location ON blocks(world, x, y, z);").execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Access> loadAccess(String type) {
        return Optional.empty();
    }

    @Override
    public List<Access> loadAccess() {
        return null;
    }

    @Override
    public void saveAccess(Access access) {

    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(BlockLocation location) {
        return Optional.empty();
    }

    @Override
    public List<BlockProtection> loadBlockProtections() {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement selectBlocks = connection.prepareStatement("SELECT * FROM blocks;")) {
                final ResultSet blocksResultSet = selectBlocks.executeQuery();
                final List<BlockProtection> protections = new ArrayList<>();
                while (blocksResultSet.next()) {
                    final String id = blocksResultSet.getString(1);
                    final String owner = blocksResultSet.getString(2);
                    final String type = blocksResultSet.getString(3);
                    final String block = blocksResultSet.getString(5);
                    final String world = blocksResultSet.getString(6);
                    final int x = blocksResultSet.getInt(7);
                    final int y = blocksResultSet.getInt(8);
                    final int z = blocksResultSet.getInt(9);
                    final String accessListString = blocksResultSet.getString(4);
                    final Map<Source, String> accessList = new HashMap<>();
                    if (!accessListString.isEmpty()) {
                        String[] accessListSplit = accessListString.split(",");
                        for (String accessListEntry : accessListSplit) {
                            String[] keyValue = accessListEntry.split(":");
                            String[] sourceTypeIdentifier = keyValue[0].split(";");
                            String sourceType = sourceTypeIdentifier[0];
                            String sourceIdentifier = sourceTypeIdentifier[1];
                            String access = keyValue[1];
                            accessList.put(new Source(sourceType, sourceIdentifier), access);
                        }
                    }
                    protections.add(new BlockProtection(UUID.fromString(id), owner, type, accessList, block, world, x, y, z));
                }
                return protections;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement insertBlock = connection.prepareStatement("INSERT INTO blocks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;")) {
                insertBlock.setString(1, protection.getId().toString());
                insertBlock.setString(2, protection.getOwner());
                insertBlock.setString(3, protection.getType());
                insertBlock.setString(4, protection.getAccessList().entrySet().stream().map(entry -> "%s;%s:%s".formatted(entry.getKey().type(), entry.getKey().identifier().replace(",", ""), entry.getValue())).collect(Collectors.joining(",")));
                insertBlock.setString(5, protection.getBlock());
                insertBlock.setString(6, protection.getWorld());
                insertBlock.setInt(7, protection.getX());
                insertBlock.setInt(8, protection.getY());
                insertBlock.setInt(9, protection.getZ());
                insertBlock.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<EntityProtection> loadEntityProtection(UUID id) {
        return Optional.empty();
    }

    @Override
    public List<EntityProtection> loadEntityProtections() {
        return null;
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {

    }
}

package org.popcraft.bolt.data.store;

import org.apache.commons.lang.time.StopWatch;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

public class SQLiteStore implements Store {
    private static final String JDBC_SQLITE_URL = "jdbc:sqlite:bolt.db";

    public SQLiteStore() {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS blocks (id varchar(36) PRIMARY KEY, owner varchar(36), type varchar(128), accesslist text, block varchar(128), world varchar(128), x integer, y integer, z integer);")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE INDEX IF NOT EXISTS block_owner ON blocks(owner);")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE UNIQUE INDEX IF NOT EXISTS block_location ON blocks(world, x, y, z);")) {
                statement.execute();
            }
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveAccess(Access access) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(UUID id) {
        return Optional.empty();
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(BlockLocation location) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement selectBlock = connection.prepareStatement("SELECT * FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
                selectBlock.setString(1, location.world());
                selectBlock.setInt(2, location.x());
                selectBlock.setInt(3, location.y());
                selectBlock.setInt(4, location.z());
                final ResultSet blockResultSet = selectBlock.executeQuery();
                if (blockResultSet.next()) {
                    LogManager.getLogManager().getLogger("").info(() -> "Loading block protection took %d ms".formatted(stopWatch.getTime()));
                    return Optional.of(blockProtectionFromResultSet(blockResultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<BlockProtection> loadBlockProtections() {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement selectBlocks = connection.prepareStatement("SELECT * FROM blocks;")) {
                final ResultSet blocksResultSet = selectBlocks.executeQuery();
                final List<BlockProtection> protections = new ArrayList<>();
                while (blocksResultSet.next()) {
                    protections.add(blockProtectionFromResultSet(blocksResultSet));
                }
                return protections;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private BlockProtection blockProtectionFromResultSet(final ResultSet resultSet) throws SQLException {
        final String id = resultSet.getString(1);
        final String owner = resultSet.getString(2);
        final String type = resultSet.getString(3);
        final String block = resultSet.getString(5);
        final String world = resultSet.getString(6);
        final int x = resultSet.getInt(7);
        final int y = resultSet.getInt(8);
        final int z = resultSet.getInt(9);
        final String accessListString = resultSet.getString(4);
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
        return new BlockProtection(UUID.fromString(id), UUID.fromString(owner), type, accessList, block, world, x, y, z);
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement insertBlock = connection.prepareStatement("INSERT INTO blocks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;")) {
                insertBlock.setString(1, protection.getId().toString());
                insertBlock.setString(2, protection.getOwner().toString());
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
    public void removeBlockProtection(BlockProtection protection) {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement deleteBlock = connection.prepareStatement("DELETE FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
                deleteBlock.setString(1, protection.getWorld());
                deleteBlock.setInt(2, protection.getX());
                deleteBlock.setInt(3, protection.getY());
                deleteBlock.setInt(4, protection.getZ());
                deleteBlock.execute();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        throw new UnsupportedOperationException();
    }
}

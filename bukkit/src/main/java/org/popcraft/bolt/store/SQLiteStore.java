package org.popcraft.bolt.store;

import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.Metrics;
import org.popcraft.bolt.util.Source;

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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

public class SQLiteStore implements Store {
    private static final String JDBC_SQLITE_URL = "jdbc:sqlite:bolt.db";

    public SQLiteStore() {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS blocks (id varchar(36) PRIMARY KEY, owner varchar(36), parent varchar(36), type varchar(128), access text, block varchar(128), world varchar(128), x integer, y integer, z integer);")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS entities (id varchar(36) PRIMARY KEY, owner varchar(36), parent varchar(36), type varchar(128), access text, entity varchar(128));")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE INDEX IF NOT EXISTS block_owner ON blocks(owner);")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE UNIQUE INDEX IF NOT EXISTS block_location ON blocks(world, x, y, z);")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE INDEX IF NOT EXISTS block_parent ON blocks(parent);")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE INDEX IF NOT EXISTS entity_parent ON entities(parent);")) {
                statement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(UUID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<BlockProtection> loadBlockProtection(BlockLocation location) {
        final long startTimeNanos = System.nanoTime();
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement selectBlock = connection.prepareStatement("SELECT * FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
                selectBlock.setString(1, location.world());
                selectBlock.setInt(2, location.x());
                selectBlock.setInt(3, location.y());
                selectBlock.setInt(4, location.z());
                final ResultSet blockResultSet = selectBlock.executeQuery();
                if (blockResultSet.next()) {
                    final long timeNanos = System.nanoTime() - startTimeNanos;
                    final double timeMillis = timeNanos / 1e6d;
                    LogManager.getLogManager().getLogger("").info(() -> "Loading block protection took %.3f ms".formatted(timeMillis));
                    Metrics.recordProtectionAccess(true);
                    return Optional.of(blockProtectionFromResultSet(blockResultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Metrics.recordProtectionAccess(false);
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
        final String parent = resultSet.getString(3);
        final String type = resultSet.getString(4);
        final String accessString = resultSet.getString(5);
        final Map<Source, String> accessMap = new HashMap<>();
        if (!accessString.isEmpty()) {
            String[] accessSplit = accessString.split(",");
            for (String accessEntry : accessSplit) {
                String[] keyValue = accessEntry.split(":");
                String[] sourceTypeIdentifier = keyValue[0].split(";");
                String sourceType = sourceTypeIdentifier[0];
                String sourceIdentifier = sourceTypeIdentifier[1];
                String access = keyValue[1];
                accessMap.put(new Source(sourceType, sourceIdentifier), access);
            }
        }
        final String block = resultSet.getString(6);
        final String world = resultSet.getString(7);
        final int x = resultSet.getInt(8);
        final int y = resultSet.getInt(9);
        final int z = resultSet.getInt(10);
        return new BlockProtection(UUID.fromString(id), UUID.fromString(owner), parent.isEmpty() ? null : UUID.fromString(parent), type, accessMap, block, world, x, y, z);
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement replaceBlock = connection.prepareStatement("REPLACE INTO blocks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                replaceBlock.setString(1, protection.getId().toString());
                replaceBlock.setString(2, protection.getOwner().toString());
                replaceBlock.setString(3, String.valueOf(Objects.requireNonNullElse(protection.getParent(), "")));
                replaceBlock.setString(4, protection.getType());
                replaceBlock.setString(5, protection.getAccess().entrySet().stream().map(entry -> "%s;%s:%s".formatted(entry.getKey().type(), entry.getKey().identifier().replace(",", ""), entry.getValue())).collect(Collectors.joining(",")));
                replaceBlock.setString(6, protection.getBlock());
                replaceBlock.setString(7, protection.getWorld());
                replaceBlock.setInt(8, protection.getX());
                replaceBlock.setInt(9, protection.getY());
                replaceBlock.setInt(10, protection.getZ());
                replaceBlock.execute();
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
        final long startTimeNanos = System.nanoTime();
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement selectEntity = connection.prepareStatement("SELECT * FROM entities WHERE id = ?;")) {
                selectEntity.setString(1, id.toString());
                final ResultSet entityResultSet = selectEntity.executeQuery();
                if (entityResultSet.next()) {
                    final long timeNanos = System.nanoTime() - startTimeNanos;
                    final double timeMillis = timeNanos / 1e6d;
                    LogManager.getLogManager().getLogger("").info(() -> "Loading entity protection took %.3f ms".formatted(timeMillis));
                    Metrics.recordProtectionAccess(true);
                    return Optional.of(entityProtectionFromResultSet(entityResultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Metrics.recordProtectionAccess(false);
        return Optional.empty();
    }

    @Override
    public List<EntityProtection> loadEntityProtections() {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement selectEntities = connection.prepareStatement("SELECT * FROM entities;")) {
                final ResultSet entitiesResultSet = selectEntities.executeQuery();
                final List<EntityProtection> protections = new ArrayList<>();
                while (entitiesResultSet.next()) {
                    protections.add(entityProtectionFromResultSet(entitiesResultSet));
                }
                return protections;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private EntityProtection entityProtectionFromResultSet(final ResultSet resultSet) throws SQLException {
        final String id = resultSet.getString(1);
        final String owner = resultSet.getString(2);
        final String parent = resultSet.getString(3);
        final String type = resultSet.getString(4);
        final String accessString = resultSet.getString(5);
        final Map<Source, String> accessMap = new HashMap<>();
        if (!accessString.isEmpty()) {
            String[] accessSplit = accessString.split(",");
            for (String accessEntry : accessSplit) {
                String[] keyValue = accessEntry.split(":");
                String[] sourceTypeIdentifier = keyValue[0].split(";");
                String sourceType = sourceTypeIdentifier[0];
                String sourceIdentifier = sourceTypeIdentifier[1];
                String access = keyValue[1];
                accessMap.put(new Source(sourceType, sourceIdentifier), access);
            }
        }
        final String entity = resultSet.getString(6);
        return new EntityProtection(UUID.fromString(id), UUID.fromString(owner), parent.isEmpty() ? null : UUID.fromString(parent), type, accessMap, entity);
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement replaceEntity = connection.prepareStatement("REPLACE INTO entities VALUES (?, ?, ?, ?, ?, ?);")) {
                replaceEntity.setString(1, protection.getId().toString());
                replaceEntity.setString(2, protection.getOwner().toString());
                replaceEntity.setString(3, String.valueOf(Objects.requireNonNullElse(protection.getParent(), "")));
                replaceEntity.setString(4, protection.getType());
                replaceEntity.setString(5, protection.getAccess().entrySet().stream().map(entry -> "%s;%s:%s".formatted(entry.getKey().type(), entry.getKey().identifier().replace(",", ""), entry.getValue())).collect(Collectors.joining(",")));
                replaceEntity.setString(6, protection.getEntity());
                replaceEntity.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        try (final Connection connection = DriverManager.getConnection(JDBC_SQLITE_URL)) {
            try (final PreparedStatement deleteEntity = connection.prepareStatement("DELETE FROM entities WHERE id = ?;")) {
                deleteEntity.setString(1, protection.getId().toString());
                deleteEntity.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

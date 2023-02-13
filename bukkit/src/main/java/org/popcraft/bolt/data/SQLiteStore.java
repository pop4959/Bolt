package org.popcraft.bolt.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.Metrics;

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
import java.util.UUID;
import java.util.logging.LogManager;

public class SQLiteStore implements Store {
    private static final Gson GSON = new Gson();
    private final String jdbcSqliteUrl;

    public SQLiteStore(final String directory) {
        jdbcSqliteUrl = "jdbc:sqlite:%s/bolt.db".formatted(directory);
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS blocks (id varchar(36) PRIMARY KEY, owner varchar(36), type varchar(128), access text, block varchar(128), world varchar(128), x integer, y integer, z integer);")) {
                statement.execute();
            }
            try (final PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS entities (id varchar(36) PRIMARY KEY, owner varchar(36), type varchar(128), access text, entity varchar(128));")) {
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
    public BlockProtection loadBlockProtection(BlockLocation location) {
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement selectBlock = connection.prepareStatement("SELECT * FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
                selectBlock.setString(1, location.world());
                selectBlock.setInt(2, location.x());
                selectBlock.setInt(3, location.y());
                selectBlock.setInt(4, location.z());
                final ResultSet blockResultSet = selectBlock.executeQuery();
                if (blockResultSet.next()) {
                    return blockProtectionFromResultSet(blockResultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Metrics.recordProtectionAccess(false);
        return null;
    }

    @Override
    public List<BlockProtection> loadBlockProtections() {
        final long startTimeNanos = System.nanoTime();
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement selectBlocks = connection.prepareStatement("SELECT * FROM blocks;")) {
                final ResultSet blocksResultSet = selectBlocks.executeQuery();
                final List<BlockProtection> protections = new ArrayList<>();
                while (blocksResultSet.next()) {
                    protections.add(blockProtectionFromResultSet(blocksResultSet));
                }
                final long timeNanos = System.nanoTime() - startTimeNanos;
                final double timeMillis = timeNanos / 1e6d;
                LogManager.getLogManager().getLogger("").info(() -> "Loading all block protections took %.3f ms".formatted(timeMillis));
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
        final String accessText = resultSet.getString(4);
        final Map<String, String> access = Objects.requireNonNullElse(GSON.fromJson(accessText, new TypeToken<HashMap<String, String>>() {
        }.getType()), new HashMap<>());
        final String block = resultSet.getString(5);
        final String world = resultSet.getString(6);
        final int x = resultSet.getInt(7);
        final int y = resultSet.getInt(8);
        final int z = resultSet.getInt(9);
        return new BlockProtection(UUID.fromString(id), UUID.fromString(owner), type, access, block, world, x, y, z);
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement replaceBlock = connection.prepareStatement("REPLACE INTO blocks VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                replaceBlock.setString(1, protection.getId().toString());
                replaceBlock.setString(2, protection.getOwner().toString());
                replaceBlock.setString(3, protection.getType());
                replaceBlock.setString(4, GSON.toJson(protection.getAccess(), new TypeToken<Map<String, String>>() {
                }.getType()));
                replaceBlock.setString(5, protection.getBlock());
                replaceBlock.setString(6, protection.getWorld());
                replaceBlock.setInt(7, protection.getX());
                replaceBlock.setInt(8, protection.getY());
                replaceBlock.setInt(9, protection.getZ());
                replaceBlock.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
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
    public EntityProtection loadEntityProtection(UUID id) {
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement selectEntity = connection.prepareStatement("SELECT * FROM entities WHERE id = ?;")) {
                selectEntity.setString(1, id.toString());
                final ResultSet entityResultSet = selectEntity.executeQuery();
                if (entityResultSet.next()) {
                    return entityProtectionFromResultSet(entityResultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Metrics.recordProtectionAccess(false);
        return null;
    }

    @Override
    public List<EntityProtection> loadEntityProtections() {
        final long startTimeNanos = System.nanoTime();
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement selectEntities = connection.prepareStatement("SELECT * FROM entities;")) {
                final ResultSet entitiesResultSet = selectEntities.executeQuery();
                final List<EntityProtection> protections = new ArrayList<>();
                while (entitiesResultSet.next()) {
                    protections.add(entityProtectionFromResultSet(entitiesResultSet));
                }
                final long timeNanos = System.nanoTime() - startTimeNanos;
                final double timeMillis = timeNanos / 1e6d;
                LogManager.getLogManager().getLogger("").info(() -> "Loading all entity protections took %.3f ms".formatted(timeMillis));
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
        final String type = resultSet.getString(3);
        final String accessText = resultSet.getString(4);
        final Map<String, String> access = Objects.requireNonNullElse(GSON.fromJson(accessText, new TypeToken<HashMap<String, String>>() {
        }.getType()), new HashMap<>());
        final String entity = resultSet.getString(5);
        return new EntityProtection(UUID.fromString(id), UUID.fromString(owner), type, access, entity);
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement replaceEntity = connection.prepareStatement("REPLACE INTO entities VALUES (?, ?, ?, ?, ?);")) {
                replaceEntity.setString(1, protection.getId().toString());
                replaceEntity.setString(2, protection.getOwner().toString());
                replaceEntity.setString(3, protection.getType());
                replaceEntity.setString(4, GSON.toJson(protection.getAccess(), new TypeToken<Map<String, String>>() {
                }.getType()));
                replaceEntity.setString(5, protection.getEntity());
                replaceEntity.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        try (final Connection connection = DriverManager.getConnection(jdbcSqliteUrl)) {
            try (final PreparedStatement deleteEntity = connection.prepareStatement("DELETE FROM entities WHERE id = ?;")) {
                deleteEntity.setString(1, protection.getId().toString());
                deleteEntity.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

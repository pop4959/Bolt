package org.popcraft.bolt.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Metrics;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;

public class SQLiteStore implements Store {
    private static final Gson GSON = new Gson();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<BlockLocation, BlockProtection> saveBlocks = new HashMap<>();
    private final Map<BlockLocation, BlockProtection> removeBlocks = new HashMap<>();
    private final Map<UUID, EntityProtection> saveEntities = new HashMap<>();
    private final Map<UUID, EntityProtection> removeEntities = new HashMap<>();
    private Connection connection;

    public SQLiteStore(final String directory) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:%s/bolt.db".formatted(directory));
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
        executor.scheduleWithFixedDelay(this::flush, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<BlockProtection> loadBlockProtection(BlockLocation location) {
        final CompletableFuture<BlockProtection> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (final PreparedStatement selectBlock = connection.prepareStatement("SELECT * FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
                selectBlock.setString(1, location.world());
                selectBlock.setInt(2, location.x());
                selectBlock.setInt(3, location.y());
                selectBlock.setInt(4, location.z());
                final ResultSet blockResultSet = selectBlock.executeQuery();
                if (blockResultSet.next()) {
                    future.complete(blockProtectionFromResultSet(blockResultSet));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Metrics.recordProtectionAccess(false);
            future.complete(null);
        }, executor);
        return future;
    }

    @Override
    public CompletableFuture<Collection<BlockProtection>> loadBlockProtections() {
        final CompletableFuture<Collection<BlockProtection>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            final long startTimeNanos = System.nanoTime();
            try (final PreparedStatement selectBlocks = connection.prepareStatement("SELECT * FROM blocks;")) {
                final ResultSet blocksResultSet = selectBlocks.executeQuery();
                final List<BlockProtection> protections = new ArrayList<>();
                while (blocksResultSet.next()) {
                    protections.add(blockProtectionFromResultSet(blocksResultSet));
                }
                final long timeNanos = System.nanoTime() - startTimeNanos;
                final double timeMillis = timeNanos / 1e6d;
                LogManager.getLogManager().getLogger("").info(() -> "Loading all block protections took %.3f ms".formatted(timeMillis));
                future.complete(protections);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            future.complete(Collections.emptyList());
        }, executor);
        return future;
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
        CompletableFuture.runAsync(() -> saveBlocks.put(BukkitAdapter.blockLocation(protection), protection), executor);
    }

    private void saveBlockProtectionNow(BlockProtection protection) {
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
            replaceBlock.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        CompletableFuture.runAsync(() -> removeBlocks.put(BukkitAdapter.blockLocation(protection), protection), executor);
    }

    private void removeBlockProtectionNow(BlockProtection protection) {
        try (final PreparedStatement deleteBlock = connection.prepareStatement("DELETE FROM blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;")) {
            deleteBlock.setString(1, protection.getWorld());
            deleteBlock.setInt(2, protection.getX());
            deleteBlock.setInt(3, protection.getY());
            deleteBlock.setInt(4, protection.getZ());
            deleteBlock.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<EntityProtection> loadEntityProtection(UUID id) {
        final CompletableFuture<EntityProtection> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (final PreparedStatement selectEntity = connection.prepareStatement("SELECT * FROM entities WHERE id = ?;")) {
                selectEntity.setString(1, id.toString());
                final ResultSet entityResultSet = selectEntity.executeQuery();
                if (entityResultSet.next()) {
                    future.complete(entityProtectionFromResultSet(entityResultSet));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Metrics.recordProtectionAccess(false);
            future.complete(null);
        }, executor);
        return future;
    }

    @Override
    public CompletableFuture<Collection<EntityProtection>> loadEntityProtections() {
        final CompletableFuture<Collection<EntityProtection>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            final long startTimeNanos = System.nanoTime();
            try (final PreparedStatement selectEntities = connection.prepareStatement("SELECT * FROM entities;")) {
                final ResultSet entitiesResultSet = selectEntities.executeQuery();
                final List<EntityProtection> protections = new ArrayList<>();
                while (entitiesResultSet.next()) {
                    protections.add(entityProtectionFromResultSet(entitiesResultSet));
                }
                final long timeNanos = System.nanoTime() - startTimeNanos;
                final double timeMillis = timeNanos / 1e6d;
                LogManager.getLogManager().getLogger("").info(() -> "Loading all entity protections took %.3f ms".formatted(timeMillis));
                future.complete(protections);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            future.complete(Collections.emptyList());
        }, executor);
        return future;
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
        CompletableFuture.runAsync(() -> saveEntities.put(protection.getId(), protection), executor);
    }

    private void saveEntityProtectionNow(EntityProtection protection) {
        try (final PreparedStatement replaceEntity = connection.prepareStatement("REPLACE INTO entities VALUES (?, ?, ?, ?, ?);")) {
            replaceEntity.setString(1, protection.getId().toString());
            replaceEntity.setString(2, protection.getOwner().toString());
            replaceEntity.setString(3, protection.getType());
            replaceEntity.setString(4, GSON.toJson(protection.getAccess(), new TypeToken<Map<String, String>>() {
            }.getType()));
            replaceEntity.setString(5, protection.getEntity());
            replaceEntity.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeEntityProtection(EntityProtection protection) {
        CompletableFuture.runAsync(() -> removeEntities.put(protection.getId(), protection), executor);
    }

    private void removeEntityProtectionNow(EntityProtection protection) {
        try (final PreparedStatement deleteEntity = connection.prepareStatement("DELETE FROM entities WHERE id = ?;")) {
            deleteEntity.setString(1, protection.getId().toString());
            deleteEntity.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> flush() {
        final CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                if (!saveBlocks.isEmpty()) {
                    connection.setAutoCommit(false);
                    final Iterator<BlockProtection> saveBlocksIterator = saveBlocks.values().iterator();
                    while (saveBlocksIterator.hasNext()) {
                        saveBlockProtectionNow(saveBlocksIterator.next());
                        saveBlocksIterator.remove();
                    }
                    connection.setAutoCommit(true);
                }
                if (!removeBlocks.isEmpty()) {
                    connection.setAutoCommit(false);
                    final Iterator<BlockProtection> removeBlocksIterator = removeBlocks.values().iterator();
                    while (removeBlocksIterator.hasNext()) {
                        removeBlockProtectionNow(removeBlocksIterator.next());
                        removeBlocksIterator.remove();
                    }
                    connection.setAutoCommit(true);
                }
                if (!saveEntities.isEmpty()) {
                    connection.setAutoCommit(false);
                    final Iterator<EntityProtection> saveEntitiesIterator = saveEntities.values().iterator();
                    while (saveEntitiesIterator.hasNext()) {
                        saveEntityProtectionNow(saveEntitiesIterator.next());
                        saveEntitiesIterator.remove();
                    }
                    connection.setAutoCommit(true);
                }
                if (!removeEntities.isEmpty()) {
                    connection.setAutoCommit(false);
                    final Iterator<EntityProtection> removeEntitiesIterator = removeEntities.values().iterator();
                    while (removeEntitiesIterator.hasNext()) {
                        removeEntityProtectionNow(removeEntitiesIterator.next());
                        removeEntitiesIterator.remove();
                    }
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                completionFuture.complete(null);
            }
        }, executor);
        return completionFuture;
    }
}

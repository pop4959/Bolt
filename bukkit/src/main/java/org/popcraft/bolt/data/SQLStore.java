package org.popcraft.bolt.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.popcraft.bolt.data.sql.Statements;
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

public class SQLStore implements Store {
    private static final Gson GSON = new Gson();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Map<BlockLocation, BlockProtection> saveBlocks = new HashMap<>();
    private final Map<BlockLocation, BlockProtection> removeBlocks = new HashMap<>();
    private final Map<UUID, EntityProtection> saveEntities = new HashMap<>();
    private final Map<UUID, EntityProtection> removeEntities = new HashMap<>();
    private final Configuration configuration;
    private final String connectionUrl;
    private Connection connection;

    public SQLStore(final Configuration configuration) {
        this.configuration = configuration;
        final boolean usingMySQL = "mysql".equals(configuration.type());
        this.connectionUrl = usingMySQL ?
                "jdbc:mysql://%s:%s@%s/%s".formatted(configuration.username(), configuration.password(), configuration.hostname(), configuration.database()) :
                "jdbc:sqlite:%s".formatted(configuration.path());
        reconnect();
        try (final PreparedStatement createBlocksTable = connection.prepareStatement(Statements.CREATE_TABLE_BLOCKS.get(configuration.type()).formatted(configuration.prefix()));
             final PreparedStatement createEntitiesTable = connection.prepareStatement(Statements.CREATE_TABLE_ENTITIES.get(configuration.type()).formatted(configuration.prefix()))) {
            createBlocksTable.execute();
            createEntitiesTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try (final PreparedStatement createBlocksOwnerIndex = connection.prepareStatement(Statements.CREATE_INDEX_BLOCK_OWNER.get(configuration.type()).formatted(configuration.prefix()));
             final PreparedStatement createBlocksLocationIndex = connection.prepareStatement(Statements.CREATE_INDEX_BLOCK_LOCATION.get(configuration.type()).formatted(configuration.prefix()));
             final PreparedStatement createEntitiesOwnerIndex = connection.prepareStatement(Statements.CREATE_INDEX_ENTITY_OWNER.get(configuration.type()).formatted(configuration.prefix()))) {
            createBlocksOwnerIndex.execute();
            createBlocksLocationIndex.execute();
            createEntitiesOwnerIndex.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        executor.scheduleWithFixedDelay(this::flush, 30, 30, TimeUnit.SECONDS);
        if (usingMySQL) {
            executor.scheduleWithFixedDelay(this::reconnect, 30, 30, TimeUnit.MINUTES);
        }
    }

    public record Configuration(String type, String path, String hostname, String database, String username,
                                String password, String prefix, List<String> properties) {
    }

    private void reconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
            connection = DriverManager.getConnection(connectionUrl);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<BlockProtection> loadBlockProtection(BlockLocation location) {
        final CompletableFuture<BlockProtection> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try (final PreparedStatement selectBlock = connection.prepareStatement(Statements.SELECT_BLOCK_BY_LOCATION.get(configuration.type()).formatted(configuration.prefix()))) {
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
            long[] count = new long[1];
            try (final PreparedStatement selectBlocks = connection.prepareStatement(Statements.SELECT_ALL_BLOCKS.get(configuration.type()).formatted(configuration.prefix()))) {
                final ResultSet blocksResultSet = selectBlocks.executeQuery();
                final List<BlockProtection> protections = new ArrayList<>();
                while (blocksResultSet.next()) {
                    protections.add(blockProtectionFromResultSet(blocksResultSet));
                    ++count[0];
                }
                final long timeNanos = System.nanoTime() - startTimeNanos;
                final double timeMillis = timeNanos / 1e6d;
                LogManager.getLogManager().getLogger("").info(() -> "Loaded %d block protections in %.3f ms".formatted(count[0], timeMillis));
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
        final long created = resultSet.getLong(4);
        final long accessed = resultSet.getLong(5);
        final String accessText = resultSet.getString(6);
        final Map<String, String> access = Objects.requireNonNullElse(GSON.fromJson(accessText, new TypeToken<HashMap<String, String>>() {
        }.getType()), new HashMap<>());
        final String world = resultSet.getString(7);
        final int x = resultSet.getInt(8);
        final int y = resultSet.getInt(9);
        final int z = resultSet.getInt(10);
        final String block = resultSet.getString(11);
        return new BlockProtection(UUID.fromString(id), UUID.fromString(owner), type, created, accessed, access, world, x, y, z, block);
    }

    @Override
    public void saveBlockProtection(BlockProtection protection) {
        CompletableFuture.runAsync(() -> saveBlocks.put(BukkitAdapter.blockLocation(protection), protection), executor);
    }

    private void saveBlockProtectionNow(BlockProtection protection) {
        try (final PreparedStatement replaceBlock = connection.prepareStatement(Statements.REPLACE_BLOCK.get(configuration.type()).formatted(configuration.prefix()))) {
            replaceBlock.setString(1, protection.getId().toString());
            replaceBlock.setString(2, protection.getOwner().toString());
            replaceBlock.setString(3, protection.getType());
            replaceBlock.setLong(4, protection.getCreated());
            replaceBlock.setLong(5, protection.getAccessed());
            replaceBlock.setString(6, GSON.toJson(protection.getAccess(), new TypeToken<Map<String, String>>() {
            }.getType()));
            replaceBlock.setString(7, protection.getWorld());
            replaceBlock.setInt(8, protection.getX());
            replaceBlock.setInt(9, protection.getY());
            replaceBlock.setInt(10, protection.getZ());
            replaceBlock.setString(11, protection.getBlock());
            replaceBlock.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeBlockProtection(BlockProtection protection) {
        CompletableFuture.runAsync(() -> removeBlocks.put(BukkitAdapter.blockLocation(protection), protection), executor);
    }

    private void removeBlockProtectionNow(BlockProtection protection) {
        try (final PreparedStatement deleteBlock = connection.prepareStatement(Statements.DELETE_BLOCK.get(configuration.type()).formatted(configuration.prefix()))) {
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
            try (final PreparedStatement selectEntity = connection.prepareStatement(Statements.SELECT_ENTITY_BY_UUID.get(configuration.type()).formatted(configuration.prefix()))) {
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
            long[] count = new long[1];
            try (final PreparedStatement selectEntities = connection.prepareStatement(Statements.SELECT_ALL_ENTITIES.get(configuration.type()).formatted(configuration.prefix()))) {
                final ResultSet entitiesResultSet = selectEntities.executeQuery();
                final List<EntityProtection> protections = new ArrayList<>();
                while (entitiesResultSet.next()) {
                    protections.add(entityProtectionFromResultSet(entitiesResultSet));
                    ++count[0];
                }
                final long timeNanos = System.nanoTime() - startTimeNanos;
                final double timeMillis = timeNanos / 1e6d;
                LogManager.getLogManager().getLogger("").info(() -> "Loaded %d entity protections in %.3f ms".formatted(count[0], timeMillis));
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
        final long created = resultSet.getLong(4);
        final long accessed = resultSet.getLong(5);
        final String accessText = resultSet.getString(6);
        final Map<String, String> access = Objects.requireNonNullElse(GSON.fromJson(accessText, new TypeToken<HashMap<String, String>>() {
        }.getType()), new HashMap<>());
        final String entity = resultSet.getString(7);
        return new EntityProtection(UUID.fromString(id), UUID.fromString(owner), type, created, accessed, access, entity);
    }

    @Override
    public void saveEntityProtection(EntityProtection protection) {
        CompletableFuture.runAsync(() -> saveEntities.put(protection.getId(), protection), executor);
    }

    private void saveEntityProtectionNow(EntityProtection protection) {
        try (final PreparedStatement replaceEntity = connection.prepareStatement(Statements.REPLACE_ENTITY.get(configuration.type()).formatted(configuration.prefix()))) {
            replaceEntity.setString(1, protection.getId().toString());
            replaceEntity.setString(2, protection.getOwner().toString());
            replaceEntity.setString(3, protection.getType());
            replaceEntity.setLong(4, protection.getCreated());
            replaceEntity.setLong(5, protection.getAccessed());
            replaceEntity.setString(6, GSON.toJson(protection.getAccess(), new TypeToken<Map<String, String>>() {
            }.getType()));
            replaceEntity.setString(7, protection.getEntity());
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
        try (final PreparedStatement deleteEntity = connection.prepareStatement(Statements.DELETE_ENTITY.get(configuration.type()).formatted(configuration.prefix()))) {
            deleteEntity.setString(1, protection.getId().toString());
            deleteEntity.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public long pendingSave() {
        return CompletableFuture.supplyAsync(() -> saveBlocks.size() + removeBlocks.size() + saveEntities.size() + removeEntities.size(), executor).join();
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

package org.popcraft.bolt.data.migration.lwc;

import com.google.gson.Gson;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.BlockProtection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BoltMigration {
    private final BoltPlugin plugin;

    public BoltMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public void convert() {
        final Map<String, Integer> blockIds = new HashMap<>();
        final AtomicInteger blockId = new AtomicInteger();
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
            final Gson gson = new Gson();
            for (final BlockProtection blockProtection : plugin.getBolt().getStore().loadBlockProtections().join()) {
                final String protectionBlock = blockProtection.getBlock();
                if (!blockIds.containsKey(protectionBlock)) {
                    final int nextId = blockId.incrementAndGet();
                    blockIds.put(protectionBlock, nextId);
                    addBlock.setInt(1, nextId);
                    addBlock.setString(2, protectionBlock);
                    addBlock.execute();
                }
                addProtection.setString(1, blockProtection.getOwner().toString());
                addProtection.setInt(2, 2);
                addProtection.setInt(3, blockProtection.getX());
                addProtection.setInt(4, blockProtection.getY());
                addProtection.setInt(5, blockProtection.getZ());
                addProtection.setString(6, gson.toJson(new Data()));
                addProtection.setInt(7, blockIds.get(protectionBlock));
                addProtection.setString(8, blockProtection.getWorld());
                addProtection.setString(9, "");
                addProtection.setString(10, new Date(blockProtection.getCreated()).toString());
                addProtection.setLong(11, blockProtection.getAccessed());
                addProtection.setString(12, null);
                addProtection.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

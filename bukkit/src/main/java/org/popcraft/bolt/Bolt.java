package org.popcraft.bolt;

import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.lwc.Block;
import org.popcraft.bolt.lwc.Default;
import org.popcraft.bolt.lwc.History;
import org.popcraft.bolt.lwc.Internal;
import org.popcraft.bolt.lwc.LWCData;
import org.popcraft.bolt.lwc.Protection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Bolt extends JavaPlugin {

    @Override
    public void onEnable() {
        final LWCData lwcData = importFromLWC();
        getLogger().info(String.valueOf(lwcData.getHistory().size()));
        getLogger().info(String.valueOf(lwcData.getProtections().size()));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private LWCData importFromLWC() {
        final LWCData lwcData = new LWCData();
        Connection connection = null;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:lwc.db");
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                try {
                    getLogger().info("BLOCKS");
                    ResultSet blocks = statement.executeQuery("select * from lwc_blocks");
                    while (blocks.next()) {
                        lwcData.getBlocks().add(new Block(
                                blocks.getInt("id"),
                                blocks.getString("name")
                        ));
                    }
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                }

                try {
                    getLogger().info("DEFAULTS");
                    ResultSet defaults = statement.executeQuery("select * from lwc_defaults");
                    while (defaults.next()) {
                        lwcData.getDefaults().add(new Default(
                                defaults.getString("player"),
                                defaults.getString("defaultdata")
                        ));
                    }
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                }

                try {
                    getLogger().info("HISTORY");
                    ResultSet history = statement.executeQuery("select * from lwc_history");
                    while (history.next()) {
                        lwcData.getHistory().add(new History(
                                history.getInt("id"),
                                history.getInt("protectionId"),
                                history.getString("player"),
                                history.getInt("x"),
                                history.getInt("y"),
                                history.getInt("z"),
                                history.getInt("type"),
                                history.getInt("status"),
                                history.getString("metadata"),
                                history.getLong("timestamp")
                        ));
                    }
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                }

                try {
                    getLogger().info("INTERNAL");
                    ResultSet internal = statement.executeQuery("select * from lwc_internal");
                    while (internal.next()) {
                        lwcData.getInternal().add(new Internal(
                                internal.getString("name"),
                                internal.getString("value")
                        ));
                    }
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                }

                try {
                    getLogger().info("PROTECTIONS");
                    ResultSet protections = statement.executeQuery("select * from lwc_protections");
                    while (protections.next()) {
                        lwcData.getProtections().add(new Protection(
                                protections.getInt("id"),
                                protections.getString("owner"),
                                protections.getInt("type"),
                                protections.getInt("x"),
                                protections.getInt("y"),
                                protections.getInt("z"),
                                protections.getString("data"),
                                protections.getInt("blockId"),
                                protections.getString("world"),
                                protections.getString("password"),
                                protections.getString("date"),
                                protections.getLong("last_accessed")
                        ));
                    }
                } catch (SQLException e) {
                    System.err.println(e.getMessage());
                }

            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
        return lwcData;
    }
}

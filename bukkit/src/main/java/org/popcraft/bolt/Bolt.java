package org.popcraft.bolt;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.store.Store;
import org.popcraft.bolt.event.AccessEvents;
import org.popcraft.bolt.event.EnvironmentEvents;
import org.popcraft.bolt.event.RegistrationEvents;
import org.popcraft.bolt.migration.lwc.LWCMigration;

import java.util.List;

public final class Bolt extends JavaPlugin {
    private final Store store = new LWCMigration().migrate();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new AccessEvents(this), this);
        getServer().getPluginManager().registerEvents(new EnvironmentEvents(this), this);
        getServer().getPluginManager().registerEvents(new RegistrationEvents(), this);
        final List<BlockProtection> protectedBlocks = store.loadBlockProtections().stream().filter(BlockProtection.class::isInstance).map(BlockProtection.class::cast).toList();
        getLogger().info(() -> String.valueOf(protectedBlocks.size()));
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
    }

    public Store getStore() {
        return store;
    }
}

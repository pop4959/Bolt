package org.popcraft.bolt;

import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.data.protection.ProtectedBlock;
import org.popcraft.bolt.data.store.MemoryStore;
import org.popcraft.bolt.migration.lwc.LWCMigration;

import java.util.List;

public final class Bolt extends JavaPlugin {
    final MemoryStore lwcStore = new LWCMigration().migrate();

    @Override
    public void onEnable() {
        final List<ProtectedBlock> protectedBlocks = lwcStore.loadProtections().stream().filter(ProtectedBlock.class::isInstance).map(ProtectedBlock.class::cast).toList();
        getLogger().info(() -> String.valueOf(protectedBlocks.size()));
        protectedBlocks.forEach(protectedBlock -> {
            getLogger().info(() -> String.format("ID: %s, Owner: %s, Type: %s, Block: %s, Location: %s at (%d, %d, %d)",
                    protectedBlock.getId(),
                    protectedBlock.getOwner(),
                    protectedBlock.getType(),
                    protectedBlock.getBlock(),
                    protectedBlock.getWorld(),
                    protectedBlock.getX(),
                    protectedBlock.getY(),
                    protectedBlock.getZ()
                    ));
            protectedBlock.getAccess().forEach((source, access) -> getLogger().info(() -> String.format("(%s: %s) -> %s", source.type(), source.identifier(), access)));
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}

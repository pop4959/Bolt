package org.popcraft.bolt.event;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.popcraft.bolt.Bolt;
import org.popcraft.bolt.data.Permission;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.util.BlockLocation;

import java.util.Optional;

public class AccessEvents implements Listener {
    private final Bolt bolt;

    public AccessEvents(final Bolt bolt) {
        this.bolt = bolt;
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final BlockLocation location = new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        final Optional<BlockProtection> protection = bolt.getStore().loadBlockProtection(location);
        if (protection.isPresent()) {
            final BlockProtection blockProtection = protection.get();
            if (!bolt.getAccessManager().hasAccess(e.getPlayer(), blockProtection, Permission.BREAK)) {
                e.setCancelled(true);
            }
        }
    }
}

package org.popcraft.bolt.event;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BukkitAdapter;

public class EnvironmentEvents implements Listener {
    private final BoltPlugin plugin;

    public EnvironmentEvents(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStructureGrow(final StructureGrowEvent e) {
        e.getBlocks().removeIf(blockState -> {
            final BlockLocation location = BukkitAdapter.blockLocation(blockState);
            return plugin.getBolt().getStore().loadBlockProtection(location).isPresent();
        });
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        final Block block = e.getBlock();
        final BlockLocation location = BukkitAdapter.blockLocation(block);
        if (plugin.getBolt().getStore().loadBlockProtection(location).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockMultiPlace(final BlockMultiPlaceEvent e) {
        for (final BlockState blockState : e.getReplacedBlockStates()) {
            final BlockLocation location = BukkitAdapter.blockLocation(blockState);
            if (plugin.getBolt().getStore().loadBlockProtection(location).isPresent()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(final BlockFromToEvent e) {
        final Block block = e.getToBlock();
        final BlockLocation location = BukkitAdapter.blockLocation(block);
        if (plugin.getBolt().getStore().loadBlockProtection(location).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(final BlockFadeEvent e) {
        final Block block = e.getBlock();
        final BlockLocation location = BukkitAdapter.blockLocation(block);
        if (plugin.getBolt().getStore().loadBlockProtection(location).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBreakDoor(final EntityBreakDoorEvent e) {
        final Block block = e.getBlock();
        final BlockLocation location = BukkitAdapter.blockLocation(block);
        if (plugin.getBolt().getStore().loadBlockProtection(location).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(final BlockPistonRetractEvent e) {
        for (final Block block : e.getBlocks()) {
            final BlockLocation location = BukkitAdapter.blockLocation(block);
            if (plugin.getBolt().getStore().loadBlockProtection(location).isPresent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPistonExtend(final BlockPistonExtendEvent e) {
        for (final Block block : e.getBlocks()) {
            final BlockLocation location = BukkitAdapter.blockLocation(block);
            if (plugin.getBolt().getStore().loadBlockProtection(location).isPresent()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockExplode(final BlockExplodeEvent e) {
        e.blockList().removeIf(block -> {
            final BlockLocation location = BukkitAdapter.blockLocation(block);
            return plugin.getBolt().getStore().loadBlockProtection(location).isPresent();
        });
    }

    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent e) {
        e.blockList().removeIf(block -> {
            final BlockLocation location = BukkitAdapter.blockLocation(block);
            return plugin.getBolt().getStore().loadBlockProtection(location).isPresent();
        });
    }

    @EventHandler
    public void onHangingBreak(final HangingBreakEvent e) {
        // TODO: Entity event
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent e) {
        // TODO: Entity event
    }

    @EventHandler
    public void onHangingBreakByEntity(final HangingBreakByEntityEvent e) {
        // TODO: Entity event
    }

    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent e) {
        // TODO: Entity event
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        // TODO: Entity event
    }

    @EventHandler
    public void onInventoryMoveItem(final InventoryMoveItemEvent e) {
        // TODO: Inventory event
    }
}

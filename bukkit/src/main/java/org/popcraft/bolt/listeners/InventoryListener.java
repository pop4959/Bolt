package org.popcraft.bolt.listeners;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.popcraft.bolt.AccessManager;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.PlayerMeta;

import java.util.Optional;

@SuppressWarnings("ClassCanBeRecord")
public class InventoryListener implements Listener {
    private final BoltPlugin plugin;

    public InventoryListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    @SuppressWarnings("java:S2583")
    public void onInventoryOpen(final InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
            final Block block = blockInventoryHolder.getBlock();
            final Optional<BlockProtection> protection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
            if (protection.isPresent()) {
                final BlockProtection blockProtection = protection.get();
                if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.OPEN)) {
                    e.setCancelled(true);
                }
            }
        } else if (inventoryHolder instanceof final Entity entity) {
            final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
            if (protection.isPresent()) {
                final EntityProtection entityProtection = protection.get();
                if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.OPEN)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    @SuppressWarnings("java:S2583")
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final Inventory clickedInventory = e.getClickedInventory();
        // Didn't click on an inventory
        if (clickedInventory == null) {
            return;
        }
        final InventoryAction action = e.getAction();
        // No permission needed for these actions
        if (InventoryAction.CLONE_STACK.equals(action) || InventoryAction.DROP_ALL_CURSOR.equals(action) || InventoryAction.DROP_ONE_CURSOR.equals(action) || InventoryAction.NOTHING.equals(action)) {
            return;
        }
        final InventoryType clickedInventoryType = e.getClickedInventory().getType();
        // We don't care about the player's inventory, unless the action can pull from other inventories
        if (InventoryType.PLAYER.equals(clickedInventoryType) && !InventoryAction.COLLECT_TO_CURSOR.equals(action) && !InventoryAction.MOVE_TO_OTHER_INVENTORY.equals(action)) {
            return;
        }
        final Protection protection;
        final InventoryHolder inventoryHolder = e.getInventory().getHolder();
        final Store store = plugin.getBolt().getStore();
        if (inventoryHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
            final BlockLocation location = BukkitAdapter.blockLocation(blockInventoryHolder.getBlock());
            protection = store.loadBlockProtection(location).orElse(null);
        } else if (inventoryHolder instanceof final Entity entity) {
            protection = store.loadEntityProtection(entity.getUniqueId()).orElse(null);
        } else {
            return;
        }
        // There isn't a protection
        if (protection == null) {
            return;
        }
        final AccessManager accessManager = plugin.getBolt().getAccessManager();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final boolean shouldCancel = switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME -> !accessManager.hasAccess(playerMeta, protection, Permission.DEPOSIT);
            case COLLECT_TO_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT, PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> !accessManager.hasAccess(playerMeta, protection, Permission.WITHDRAW);
            case HOTBAR_MOVE_AND_READD, SWAP_WITH_CURSOR, UNKNOWN -> !accessManager.hasAccess(playerMeta, protection, Permission.DEPOSIT, Permission.WITHDRAW);
            case MOVE_TO_OTHER_INVENTORY -> !accessManager.hasAccess(playerMeta, protection, InventoryType.PLAYER.equals(clickedInventoryType) ? Permission.DEPOSIT : Permission.WITHDRAW);
            case HOTBAR_SWAP -> {
                final ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem == null) {
                    yield !accessManager.hasAccess(playerMeta, protection, Permission.DEPOSIT, Permission.WITHDRAW);
                } else if (clickedItem.getType().isAir()) {
                    yield !accessManager.hasAccess(playerMeta, protection, Permission.DEPOSIT);
                } else {
                    yield !accessManager.hasAccess(playerMeta, protection, Permission.WITHDRAW);
                }
            }
            default -> true;
        };
        if (shouldCancel) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("java:S2583")
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final Store store = plugin.getBolt().getStore();
        final AccessManager accessManager = plugin.getBolt().getAccessManager();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        for (int rawSlot : e.getRawSlots()) {
            final Inventory slotInventory = e.getView().getInventory(rawSlot);
            if (slotInventory != null && !InventoryType.PLAYER.equals(slotInventory.getType())) {
                final Protection protection;
                final InventoryHolder inventoryHolder = slotInventory.getHolder();
                if (inventoryHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
                    final BlockLocation location = BukkitAdapter.blockLocation(blockInventoryHolder.getBlock());
                    protection = store.loadBlockProtection(location).orElse(null);
                } else if (inventoryHolder instanceof final Entity entity) {
                    protection = store.loadEntityProtection(entity.getUniqueId()).orElse(null);
                } else {
                    continue;
                }
                // There isn't a protection
                if (protection == null) {
                    continue;
                }
                if (!accessManager.hasAccess(playerMeta, protection, Permission.DEPOSIT)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    @SuppressWarnings("java:S2583")
    public void onInventoryMoveItem(final InventoryMoveItemEvent e) {
        final Store store = plugin.getBolt().getStore();
        Protection sourceProtection = null;
        final InventoryHolder sourceHolder = e.getSource().getHolder();
        if (sourceHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
            final BlockLocation location = BukkitAdapter.blockLocation(blockInventoryHolder.getBlock());
            sourceProtection = store.loadBlockProtection(location).orElse(null);
        } else if (sourceHolder instanceof final Entity entity) {
            sourceProtection = store.loadEntityProtection(entity.getUniqueId()).orElse(null);
        }
        Protection destinationProtection = null;
        final InventoryHolder destinationHolder = e.getDestination().getHolder();
        if (destinationHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
            final BlockLocation location = BukkitAdapter.blockLocation(blockInventoryHolder.getBlock());
            destinationProtection = store.loadBlockProtection(location).orElse(null);
        } else if (destinationHolder instanceof final Entity entity) {
            destinationProtection = store.loadEntityProtection(entity.getUniqueId()).orElse(null);
        }
        if (sourceProtection == null && destinationProtection == null) {
            return;
        }
        final AccessManager accessManager = plugin.getBolt().getAccessManager();
        if (sourceProtection != null && destinationProtection != null) {
            final PlayerMeta sourceMeta = plugin.playerMeta(sourceProtection.getOwner());
            final PlayerMeta destinationMeta = plugin.playerMeta(destinationProtection.getOwner());
            if (!accessManager.hasAccess(sourceMeta, destinationProtection, Permission.DEPOSIT) || !accessManager.hasAccess(destinationMeta, sourceProtection, Permission.WITHDRAW)) {
                e.setCancelled(true);
            }
        } else if (sourceProtection != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTradeSelect(final TradeSelectEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player) || !(e.getInventory().getHolder() instanceof final Entity entity)) {
            return;
        }
        plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId()).ifPresent(entityProtection -> {
            final PlayerMeta playerMeta = plugin.playerMeta(player);
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.DEPOSIT)) {
                e.setCancelled(true);
            }
        });
    }
}

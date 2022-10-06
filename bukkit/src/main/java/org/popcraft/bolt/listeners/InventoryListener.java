package org.popcraft.bolt.listeners;

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
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Source;

@SuppressWarnings("ClassCanBeRecord")
public final class InventoryListener implements Listener {
    private final BoltPlugin plugin;

    public InventoryListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(final InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        final BoltPlayer boltPlayer = plugin.player(player);
        if (boltPlayer.triggeredAction()) {
            e.setCancelled(true);
            return;
        }
        final Protection protection = getHolderProtection(e.getInventory().getHolder());
        if (protection != null && !plugin.canAccess(protection, player, Permission.OPEN)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
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
        final Protection protection = getHolderProtection(e.getInventory().getHolder());
        // There isn't a protection
        if (protection == null) {
            return;
        }
        final boolean shouldCancel = switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME -> !plugin.canAccess(protection, player, Permission.DEPOSIT);
            case COLLECT_TO_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT, PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME ->
                    !plugin.canAccess(protection, player, Permission.WITHDRAW);
            case HOTBAR_MOVE_AND_READD, SWAP_WITH_CURSOR, UNKNOWN ->
                    !plugin.canAccess(protection, player, Permission.DEPOSIT, Permission.WITHDRAW);
            case MOVE_TO_OTHER_INVENTORY ->
                    !plugin.canAccess(protection, player, InventoryType.PLAYER.equals(clickedInventoryType) ? Permission.DEPOSIT : Permission.WITHDRAW);
            case HOTBAR_SWAP -> {
                final ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem == null) {
                    yield !plugin.canAccess(protection, player, Permission.DEPOSIT, Permission.WITHDRAW);
                } else if (clickedItem.getType().isAir()) {
                    yield !plugin.canAccess(protection, player, Permission.DEPOSIT);
                } else {
                    yield !plugin.canAccess(protection, player, Permission.WITHDRAW);
                }
            }
            default -> true;
        };
        if (shouldCancel) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player)) {
            return;
        }
        for (int rawSlot : e.getRawSlots()) {
            final Inventory slotInventory = e.getView().getInventory(rawSlot);
            if (slotInventory != null && !InventoryType.PLAYER.equals(slotInventory.getType())) {
                final Protection protection = getHolderProtection(slotInventory.getHolder());
                if (protection != null && !plugin.canAccess(protection, player, Permission.DEPOSIT)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(final InventoryMoveItemEvent e) {
        Protection sourceProtection = getHolderProtection(e.getSource().getHolder());
        Protection destinationProtection = getHolderProtection(e.getDestination().getHolder());
        if (sourceProtection == null && destinationProtection == null) {
            return;
        }
        if (sourceProtection != null && destinationProtection != null) {
            if (!plugin.canAccess(destinationProtection, sourceProtection.getOwner(), Permission.DEPOSIT) || !plugin.canAccess(sourceProtection, destinationProtection.getOwner(), Permission.WITHDRAW)) {
                e.setCancelled(true);
            }
        } else if (sourceProtection != null && !plugin.canAccess(sourceProtection, Source.from(Source.HOPPER, Source.HOPPER), Permission.WITHDRAW)) {
            e.setCancelled(true);
        } else if (destinationProtection != null && !plugin.canAccess(destinationProtection, Source.from(Source.HOPPER, Source.HOPPER), Permission.DEPOSIT)) {
            e.setCancelled(true);
        }
        // TODO: Improve the above 2 checks (not necessarily hopper only)
    }

    @EventHandler
    public void onTradeSelect(final TradeSelectEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player) || !(e.getInventory().getHolder() instanceof final Entity entity)) {
            return;
        }
        if (!plugin.canAccess(entity, player, Permission.DEPOSIT)) {
            e.setCancelled(true);
        }
    }

    @SuppressWarnings("java:S2583")
    private Protection getHolderProtection(final InventoryHolder inventoryHolder) {
        final Protection protection;
        if (inventoryHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
            protection = plugin.findProtection(blockInventoryHolder.getBlock()).orElse(null);
        } else if (inventoryHolder instanceof final Entity entity) {
            protection = plugin.findProtection(entity).orElse(null);
        } else {
            protection = null;
        }
        return protection;
    }
}

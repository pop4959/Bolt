package org.popcraft.bolt.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
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
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceTypeResolver;
import org.popcraft.bolt.source.SourceTypes;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Permission;

import java.util.EnumSet;
import java.util.Map;

public final class InventoryListener implements Listener {
    private static final SourceResolver BLOCK_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.BLOCK));
    private static final SourceResolver REDSTONE_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.REDSTONE));
    @SuppressWarnings("UnstableApiUsage")
    private static final Map<InventoryType, EnumSet<Material>> INVENTORY_TYPE_BLOCKS = Map.ofEntries(
            Map.entry(InventoryType.ANVIL, EnumSet.of(Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL)),
            Map.entry(InventoryType.BARREL, EnumSet.of(Material.BARREL)),
            Map.entry(InventoryType.BLAST_FURNACE, EnumSet.of(Material.BLAST_FURNACE)),
            Map.entry(InventoryType.CHEST, EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST)),
            Map.entry(InventoryType.CRAFTER, EnumSet.of(Material.CRAFTER)),
            Map.entry(InventoryType.DISPENSER, EnumSet.of(Material.DISPENSER)),
            Map.entry(InventoryType.DROPPER, EnumSet.of(Material.DROPPER)),
            Map.entry(InventoryType.FURNACE, EnumSet.of(Material.FURNACE)),
            Map.entry(InventoryType.HOPPER, EnumSet.of(Material.HOPPER)),
            Map.entry(InventoryType.SHULKER_BOX, EnumSet.of(Material.SHULKER_BOX)),
            Map.entry(InventoryType.SMOKER, EnumSet.of(Material.SMOKER))
    );
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
        final Protection protection = getInventoryProtection(e.getInventory());
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
        final Protection protection = getInventoryProtection(e.getInventory());
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
                final Protection protection = getInventoryProtection(slotInventory);
                if (protection != null && !plugin.canAccess(protection, player, Permission.DEPOSIT)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(final InventoryMoveItemEvent e) {
        final Protection sourceProtection = getInventoryProtection(e.getSource());
        final Protection destinationProtection = getInventoryProtection(e.getDestination());
        if (sourceProtection == null && destinationProtection == null) {
            return;
        }
        // Droppers can move items to another container, but they need to be activated by redstone to do so
        if (sourceProtection != null && InventoryType.DROPPER.equals(e.getSource().getType())) {
            if (!plugin.canAccess(sourceProtection, REDSTONE_SOURCE_RESOLVER, Permission.REDSTONE)) {
                e.setCancelled(true);
                return;
            }
        }
        if (sourceProtection != null && destinationProtection != null) {
            if (!plugin.canAccess(destinationProtection, sourceProtection.getOwner(), Permission.DEPOSIT) || !plugin.canAccess(sourceProtection, destinationProtection.getOwner(), Permission.WITHDRAW)) {
                e.setCancelled(true);
            }
        } else if (sourceProtection != null && !plugin.canAccess(sourceProtection, BLOCK_SOURCE_RESOLVER, Permission.WITHDRAW)) {
            e.setCancelled(true);
        } else if (destinationProtection != null && !plugin.canAccess(destinationProtection, BLOCK_SOURCE_RESOLVER, Permission.DEPOSIT)) {
            e.setCancelled(true);
        }
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

    public void onAnvilBreak(final InventoryEvent e) {
        final Protection anvilProtection = getInventoryProtection(e.getInventory());
        if (anvilProtection == null) {
            return;
        }
        plugin.removeProtection(anvilProtection);
    }

    private Protection getInventoryProtection(final Inventory inventory) {
        final InventoryType inventoryType = inventory.getType();
        final EnumSet<Material> blockTypes = INVENTORY_TYPE_BLOCKS.get(inventoryType);
        if (blockTypes != null) {
            final Location inventoryLocation = inventory.getLocation();
            if (inventoryLocation != null) {
                final Block block = inventoryLocation.getBlock();
                if (blockTypes.contains(block.getType())) {
                    return plugin.findProtection(block);
                }
            }
        }
        return getHolderProtection(inventory.getHolder());
    }

    private Protection getHolderProtection(final InventoryHolder inventoryHolder) {
        if (inventoryHolder instanceof final Entity entity) {
            return plugin.findProtection(entity);
        } else if (inventoryHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
            return plugin.findProtection(blockInventoryHolder.getBlock());
        } else if (inventoryHolder instanceof final DoubleChest doubleChest) {
            return plugin.findProtection(doubleChest.getLocation().getBlock());
        } else {
            return null;
        }
    }
}

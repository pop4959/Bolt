package org.popcraft.bolt.listeners;

import com.destroystokyo.paper.event.block.AnvilDamagedEvent;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceTypeResolver;
import org.popcraft.bolt.source.SourceTypes;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.EnumUtil;
import org.popcraft.bolt.util.Permission;

public final class InventoryListener implements Listener {
    private static final SourceResolver BLOCK_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.BLOCK));
    private static final SourceResolver REDSTONE_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.REDSTONE));

    // These exist only in newer versions of 1.21.4 and only in Paper.
    private static final InventoryAction PICKUP_FROM_BUNDLE = EnumUtil.valueOf(InventoryAction.class, "PICKUP_FROM_BUNDLE").orElse(null);
    private static final InventoryAction PICKUP_ALL_INTO_BUNDLE = EnumUtil.valueOf(InventoryAction.class, "PICKUP_ALL_INTO_BUNDLE").orElse(null);
    private static final InventoryAction PICKUP_SOME_INTO_BUNDLE = EnumUtil.valueOf(InventoryAction.class, "PICKUP_SOME_INTO_BUNDLE").orElse(null);
    private static final InventoryAction PLACE_FROM_BUNDLE = EnumUtil.valueOf(InventoryAction.class, "PLACE_FROM_BUNDLE").orElse(null);
    private static final InventoryAction PLACE_ALL_INTO_BUNDLE = EnumUtil.valueOf(InventoryAction.class, "PLACE_ALL_INTO_BUNDLE").orElse(null);
    private static final InventoryAction PLACE_SOME_INTO_BUNDLE = EnumUtil.valueOf(InventoryAction.class, "PLACE_SOME_INTO_BUNDLE").orElse(null);

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
            default -> {
                if (action == PICKUP_FROM_BUNDLE || action == PICKUP_ALL_INTO_BUNDLE || action == PICKUP_SOME_INTO_BUNDLE) {
                    yield !plugin.canAccess(protection, player, Permission.WITHDRAW);
                } else if (action == PLACE_FROM_BUNDLE || action == PLACE_ALL_INTO_BUNDLE || action == PLACE_SOME_INTO_BUNDLE) {
                    yield !plugin.canAccess(protection, player, Permission.DEPOSIT);
                }
                yield true;
            }
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAnvilDamaged(final AnvilDamagedEvent e) {
        if (!e.isBreaking()) {
            return;
        }
        final Protection anvilProtection = getInventoryProtection(e.getInventory());
        if (anvilProtection == null) {
            return;
        }
        plugin.removeProtection(anvilProtection);
    }

    private Protection getInventoryProtection(final Inventory inventory) {
        final InventoryHolder holder = inventory.getHolder(false);
        return switch (holder) {
            case final Entity entity -> plugin.findProtection(entity);
            case final BlockInventoryHolder blockInventoryHolder -> plugin.findProtection(blockInventoryHolder.getBlock());
            case final DoubleChest doubleChest -> plugin.findProtection(doubleChest.getLocation().getBlock());
            case null, default -> null;
        };
    }
}

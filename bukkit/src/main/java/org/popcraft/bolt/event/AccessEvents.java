package org.popcraft.bolt.event;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.popcraft.bolt.Bolt;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.store.Store;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Optional;

import static org.popcraft.bolt.util.lang.Translator.translate;

public class AccessEvents implements Listener {
    private final BoltPlugin plugin;

    public AccessEvents(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Block clicked = e.getClickedBlock();
        if (clicked == null) {
            return;
        }
        final Player player = e.getPlayer();
        final Bolt bolt = plugin.getBolt();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Store store = bolt.getStore();
        final Optional<BlockProtection> optionalProtection = store.loadBlockProtection(BukkitAdapter.blockLocation(clicked));
        if (playerMeta.triggerAction(Action.LOCK_BLOCK)) {
            if (optionalProtection.isPresent()) {
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_LOCKED_ALREADY);
            } else {
                store.saveBlockProtection(BukkitAdapter.createPrivateBlockProtection(clicked, player));
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_LOCKED,
                        Template.of("block", Strings.toTitleCase(clicked.getType()))
                );
            }
        } else if (playerMeta.triggerAction(Action.UNLOCK_BLOCK)) {
            if (optionalProtection.isPresent()) {
                store.removeBlockProtection(optionalProtection.get());
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_UNLOCKED,
                        Template.of("block", Strings.toTitleCase(clicked.getType()))
                );
            } else {
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_NOT_LOCKED);
            }
        } else if (playerMeta.triggerAction(Action.INFO)) {
            optionalProtection.ifPresentOrElse(protection -> BoltComponents.sendMessage(player, Translation.INFO,
                    Template.of("type", Strings.toTitleCase(protection.getType())),
                    Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
            ), () -> BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_NOT_LOCKED));
        } else if (playerMeta.triggerAction(Action.MODIFY)) {
            optionalProtection.ifPresentOrElse(protection -> {
                playerMeta.getModifications().forEach((source, type) -> {
                    if (type == null || bolt.getAccessRegistry().get(type).isEmpty()) {
                        protection.getAccessList().remove(source);
                    } else {
                        protection.getAccessList().put(source, type);
                    }
                });
                bolt.getStore().saveBlockProtection(protection);
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_MODIFIED);
            }, () -> BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_NOT_LOCKED));
            playerMeta.getModifications().clear();
        } else if (optionalProtection.isPresent()) {
            final boolean shouldSendMessage = EquipmentSlot.HAND.equals(e.getHand());
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            final BlockProtection protection = optionalProtection.get();
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, protection, Permission.INTERACT)) {
                e.setCancelled(true);
                if (shouldSendMessage && !hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Template.of("block", Strings.toTitleCase(clicked.getType())));
                }
            }
            if (shouldSendMessage && hasNotifyPermission) {
                BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY,
                        Template.of("block", Strings.toTitleCase(clicked.getType())),
                        Template.of("owner", player.getUniqueId().equals(protection.getOwner()) ? translate(Translation.YOU) : BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
                );
            }
            if (Material.LECTERN.equals(clicked.getType()) && e.getItem() != null && (Material.WRITABLE_BOOK.equals(e.getItem().getType()) || Material.WRITTEN_BOOK.equals(e.getItem().getType())) && !plugin.getBolt().getAccessManager().hasAccess(playerMeta, protection, Permission.DEPOSIT)) {
                e.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Optional<BlockProtection> protection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
        final Player player = e.getPlayer();
        if (protection.isPresent()) {
            final BlockProtection blockProtection = protection.get();
            final PlayerMeta playerMeta = plugin.playerMeta(player);
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.BREAK)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSignChange(final SignChangeEvent e) {
        final Block block = e.getBlock();
        final Optional<BlockProtection> protection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
        if (protection.isPresent()) {
            final BlockProtection blockProtection = protection.get();
            final PlayerMeta playerMeta = plugin.playerMeta(e.getPlayer());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.INTERACT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onHangingBreakByEntity(final HangingBreakByEntityEvent e) {
        final Entity entity = e.getEntity();
        final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
        if (protection.isPresent()) {
            final EntityProtection entityProtection = protection.get();
            if (!(e.getRemover() instanceof final Player player) || !plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), entityProtection, Permission.KILL)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent e) {
        final Entity entity = e.getVehicle();
        final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
        if (protection.isPresent()) {
            final EntityProtection entityProtection = protection.get();
            if (!(e.getAttacker() instanceof final Player player) || !plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), entityProtection, Permission.KILL)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(final PlayerArmorStandManipulateEvent e) {
        final Entity entity = e.getRightClicked();
        final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
        if (protection.isPresent()) {
            final EntityProtection entityProtection = protection.get();
            final PlayerMeta playerMeta = plugin.playerMeta(e.getPlayer());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.INTERACT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        final Entity entity = e.getEntity();
        final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
        if (protection.isPresent()) {
            final EntityProtection entityProtection = protection.get();
            if (!(e.getDamager() instanceof final Player player) || !plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), entityProtection, Permission.KILL)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent e) {
        final Entity entity = e.getRightClicked();
        final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
        if (protection.isPresent()) {
            final EntityProtection entityProtection = protection.get();
            final PlayerMeta playerMeta = plugin.playerMeta(e.getPlayer());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.INTERACT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
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
        }
        // TODO: Handle entities
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final InventoryAction action = e.getAction();
        final Inventory clickedInventory = e.getClickedInventory();
        // Didn't click on an inventory
        if (clickedInventory == null) {
            return;
        }
        final InventoryType clickedInventoryType = e.getClickedInventory().getType();
        // We don't care about the player's inventory, unless the action can pull from other inventories
        if (InventoryType.PLAYER.equals(clickedInventoryType) && !InventoryAction.COLLECT_TO_CURSOR.equals(action) && !InventoryAction.MOVE_TO_OTHER_INVENTORY.equals(action)) {
            return;
        }
        final Location location = e.getInventory().getLocation();
        // Not a physical inventory that can be protected
        if (location == null) {
            return;
        }
        switch (action) {
            // Add
            case PLACE_ALL, PLACE_ONE, PLACE_SOME -> plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(location)).ifPresent(blockProtection -> {
                if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.DEPOSIT)) {
                    e.setCancelled(true);
                }
            });
            // Remove
            case COLLECT_TO_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT, PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(location)).ifPresent(blockProtection -> {
                if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.WITHDRAW)) {
                    e.setCancelled(true);
                }
            });
            // Add and remove
            case HOTBAR_MOVE_AND_READD, SWAP_WITH_CURSOR, UNKNOWN -> plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(location)).ifPresent(blockProtection -> {
                if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.DEPOSIT, Permission.WITHDRAW)) {
                    e.setCancelled(true);
                }
            });
            // Add or remove
            case HOTBAR_SWAP -> plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(location)).ifPresent(blockProtection -> {
                final ItemStack clickedItem = e.getCurrentItem();
                if (clickedItem == null) {
                    if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.DEPOSIT, Permission.WITHDRAW)) {
                        e.setCancelled(true);
                    }
                } else if (clickedItem.getType().isAir()) {
                    if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.DEPOSIT)) {
                        e.setCancelled(true);
                    }
                } else if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.WITHDRAW)) {
                    e.setCancelled(true);
                }
            });
            case MOVE_TO_OTHER_INVENTORY -> plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(location)).ifPresent(blockProtection -> {
                if (InventoryType.PLAYER.equals(clickedInventoryType)) {
                    if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.DEPOSIT)) {
                        e.setCancelled(true);
                    }
                } else {
                    if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.WITHDRAW)) {
                        e.setCancelled(true);
                    }
                }
            });
            case CLONE_STACK, DROP_ALL_CURSOR, DROP_ONE_CURSOR, NOTHING -> {
                // No permission needed
            }
            // Unknown, cancel
            default -> e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final Location location = e.getInventory().getLocation();
        if (location == null) {
            return;
        }
        for (int rawSlot : e.getRawSlots()) {
            final Inventory slotInventory = e.getView().getInventory(rawSlot);
            if (slotInventory != null && !InventoryType.PLAYER.equals(slotInventory.getType())) {
                plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(location)).ifPresent(blockProtection -> {
                    if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(player), blockProtection, Permission.DEPOSIT)) {
                        e.setCancelled(true);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onPlayerTakeLecternBook(final PlayerTakeLecternBookEvent e) {
        final Block block = e.getLectern().getBlock();
        plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block)).ifPresent(blockProtection -> {
            if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(e.getPlayer()), blockProtection, Permission.WITHDRAW)) {
                e.setCancelled(true);
            }
        });
    }

    public void onPlayerRecipeBookClick(final PlayerEvent e) {
        if (!(e instanceof Cancellable cancellable)) {
            return;
        }
        final Player player = e.getPlayer();
        final Inventory inventory = player.getOpenInventory().getTopInventory();
        final Location location = inventory.getLocation();
        if (location == null) {
            return;
        }
        plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(location)).ifPresent(blockProtection -> {
            if (!plugin.getBolt().getAccessManager().hasAccess(plugin.playerMeta(e.getPlayer()), blockProtection, Permission.DEPOSIT, Permission.WITHDRAW)) {
                cancellable.setCancelled(true);
            }
        });
    }
}

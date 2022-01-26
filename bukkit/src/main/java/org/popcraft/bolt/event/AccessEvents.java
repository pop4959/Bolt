package org.popcraft.bolt.event;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.popcraft.bolt.AccessManager;
import org.popcraft.bolt.Bolt;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.store.Store;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import java.util.EnumSet;
import java.util.Optional;

import static org.popcraft.bolt.util.lang.Translator.translate;

public class AccessEvents implements Listener {
    private static final EnumSet<BlockFace> CARTESIAN_BLOCK_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
    private static final EnumSet<Material> DYES = EnumSet.of(Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE, Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE, Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE);
    // TODO: These uprooted types should be structures
    private static final EnumSet<Material> UPROOT = EnumSet.of(Material.BAMBOO, Material.CACTUS, Material.SUGAR_CANE);
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
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Bolt bolt = plugin.getBolt();
        final Store store = bolt.getStore();
        final Optional<BlockProtection> optionalProtection = store.loadBlockProtection(BukkitAdapter.blockLocation(clicked));
        if (playerMeta.triggerAction(Action.LOCK)) {
            if (optionalProtection.isPresent()) {
                BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY,
                        Template.of("type", Strings.toTitleCase(clicked.getType()))
                );
            } else {
                store.saveBlockProtection(BukkitAdapter.createPrivateBlockProtection(clicked, player));
                BoltComponents.sendMessage(player, Translation.CLICK_LOCKED,
                        Template.of("type", Strings.toTitleCase(clicked.getType()))
                );
            }
            e.setCancelled(true);
        } else if (playerMeta.triggerAction(Action.UNLOCK)) {
            if (optionalProtection.isPresent()) {
                store.removeBlockProtection(optionalProtection.get());
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED,
                        Template.of("type", Strings.toTitleCase(clicked.getType()))
                );
            } else {
                BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED,
                        Template.of("type", Strings.toTitleCase(clicked.getType()))
                );
            }
            e.setCancelled(true);
        } else if (playerMeta.triggerAction(Action.INFO)) {
            optionalProtection.ifPresentOrElse(protection -> BoltComponents.sendMessage(player, Translation.INFO,
                    Template.of("access", Strings.toTitleCase(protection.getType())),
                    Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
            ), () -> BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(clicked.getType()))));
            e.setCancelled(true);
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
                BoltComponents.sendMessage(player, Translation.CLICK_MODIFIED);
            }, () -> BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(clicked.getType()))));
            playerMeta.getModifications().clear();
            e.setCancelled(true);
        } else if (playerMeta.triggerAction(Action.DEBUG)) {
            BoltComponents.sendMessage(player, optionalProtection.map(Protection::toString).toString());
            e.setCancelled(true);
        } else if (optionalProtection.isPresent()) {
            final boolean shouldSendMessage = EquipmentSlot.HAND.equals(e.getHand());
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            final BlockProtection protection = optionalProtection.get();
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, protection, Permission.INTERACT)) {
                e.setCancelled(true);
                if (shouldSendMessage && !hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Template.of("type", Strings.toTitleCase(clicked.getType())));
                }
            }
            if (shouldSendMessage && hasNotifyPermission) {
                BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY,
                        Template.of("type", Strings.toTitleCase(clicked.getType())),
                        Template.of("owner", player.getUniqueId().equals(protection.getOwner()) ? translate(Translation.YOU) : BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
                );
            }
            if (e.getItem() != null) {
                final Material itemType = e.getItem().getType();
                if (Material.LECTERN.equals(clicked.getType()) && (Material.WRITABLE_BOOK.equals(itemType) || Material.WRITTEN_BOOK.equals(itemType)) && !plugin.getBolt().getAccessManager().hasAccess(playerMeta, protection, Permission.DEPOSIT)) {
                    e.setUseItemInHand(Event.Result.DENY);
                } else if ((Tag.SIGNS.isTagged(clicked.getType()) && (DYES.contains(itemType) || Material.GLOW_INK_SAC.equals(itemType)) && !plugin.getBolt().getAccessManager().hasAccess(playerMeta, protection, Permission.MODIFY))) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        final Block block = e.getBlock();
        final Material blockType = block.getType();
        final Player player = e.getPlayer();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        if (Material.CARVED_PUMPKIN.equals(blockType) || Material.JACK_O_LANTERN.equals(blockType)) {
            for (final BlockFace blockFace : CARTESIAN_BLOCK_FACES) {
                final Block firstBlock = block.getRelative(blockFace);
                final Block secondBlock = firstBlock.getRelative(blockFace);
                if (Material.SNOW_BLOCK.equals(firstBlock.getType()) && Material.SNOW_BLOCK.equals(secondBlock.getType())) {
                    final Optional<BlockProtection> firstProtection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(firstBlock));
                    firstProtection.ifPresent(blockProtection -> {
                        if (plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.BREAK)) {
                            plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                        } else {
                            e.setCancelled(true);
                        }
                    });
                    final Optional<BlockProtection> secondProtection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(secondBlock));
                    secondProtection.ifPresent(blockProtection -> {
                        if (plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.BREAK)) {
                            plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                        } else {
                            e.setCancelled(true);
                        }
                    });
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Material blockType = block.getType();
        final Optional<BlockProtection> optionalProtection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
        final Player player = e.getPlayer();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        if (optionalProtection.isPresent()) {
            final BlockProtection blockProtection = optionalProtection.get();
            if (plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.BREAK)) {
                plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Strings.toTitleCase(block.getType())));
            } else {
                e.setCancelled(true);
            }
        } else if (UPROOT.contains(blockType)) {
            for (Block above = block.getRelative(BlockFace.UP); UPROOT.contains(above.getType()); above = above.getRelative(BlockFace.UP)) {
                final Optional<BlockProtection> optionalAboveProtection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(above));
                if (optionalAboveProtection.isPresent()) {
                    final BlockProtection blockProtection = optionalAboveProtection.get();
                    if (plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.BREAK)) {
                        plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                    } else {
                        e.setCancelled(true);
                    }
                }
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
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, Permission.MODIFY)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        if (handlePlayerEntityInteraction(e.getPlayer(), e.getRightClicked(), false, EquipmentSlot.HAND.equals(e.getHand()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        final Entity damager = e.getDamager();
        final Entity entity = e.getEntity();
        if ((damager instanceof final Player player && handlePlayerEntityInteraction(player, entity, true, true)) || (!(damager instanceof Player) && plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId()).isPresent())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleDamage(final VehicleDamageEvent e) {
        final Entity attacker = e.getAttacker();
        final Entity vehicle = e.getVehicle();
        if (attacker instanceof final Player player && handlePlayerEntityInteraction(player, vehicle, false, true)) {
            e.setCancelled(true);
        }
    }

    private boolean handlePlayerEntityInteraction(final Player player, final Entity entity, final boolean damage, final boolean shouldSendMessage) {
        boolean shouldCancel = false;
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Bolt bolt = plugin.getBolt();
        final Store store = bolt.getStore();
        final Optional<EntityProtection> optionalProtection = store.loadEntityProtection(entity.getUniqueId());
        if (playerMeta.triggerAction(Action.LOCK)) {
            if (optionalProtection.isPresent()) {
                BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY,
                        Template.of("type", Strings.toTitleCase(entity.getType()))
                );
            } else {
                store.saveEntityProtection(BukkitAdapter.createPrivateEntityProtection(entity, player));
                BoltComponents.sendMessage(player, Translation.CLICK_LOCKED,
                        Template.of("type", Strings.toTitleCase(entity.getType()))
                );
            }
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.UNLOCK)) {
            if (optionalProtection.isPresent()) {
                store.removeEntityProtection(optionalProtection.get());
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED,
                        Template.of("type", Strings.toTitleCase(entity.getType()))
                );
            } else {
                BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED,
                        Template.of("type", Strings.toTitleCase(entity.getType()))
                );
            }
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.INFO)) {
            optionalProtection.ifPresentOrElse(protection -> BoltComponents.sendMessage(player, Translation.INFO,
                    Template.of("access", Strings.toTitleCase(protection.getType())),
                    Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
            ), () -> BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType()))));
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.MODIFY)) {
            optionalProtection.ifPresentOrElse(protection -> {
                playerMeta.getModifications().forEach((source, type) -> {
                    if (type == null || bolt.getAccessRegistry().get(type).isEmpty()) {
                        protection.getAccessList().remove(source);
                    } else {
                        protection.getAccessList().put(source, type);
                    }
                });
                bolt.getStore().saveEntityProtection(protection);
                BoltComponents.sendMessage(player, Translation.CLICK_MODIFIED);
            }, () -> BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType()))));
            playerMeta.getModifications().clear();
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.DEBUG)) {
            BoltComponents.sendMessage(player, optionalProtection.map(Protection::toString).toString());
            shouldCancel = true;
        } else if (optionalProtection.isPresent()) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            final EntityProtection protection = optionalProtection.get();
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, protection, damage ? Permission.KILL : Permission.INTERACT)) {
                shouldCancel = true;
                if (shouldSendMessage && !hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
            }
            if (shouldSendMessage && hasNotifyPermission) {
                BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY,
                        Template.of("type", Strings.toTitleCase(entity.getType())),
                        Template.of("owner", player.getUniqueId().equals(protection.getOwner()) ? translate(Translation.YOU) : BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
                );
            }
        }
        return shouldCancel;
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
    public void onPlayerArmorStandManipulate(final PlayerArmorStandManipulateEvent e) {
        final Entity entity = e.getRightClicked();
        final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
        if (protection.isPresent()) {
            final EntityProtection entityProtection = protection.get();
            final PlayerMeta playerMeta = plugin.playerMeta(e.getPlayer());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.MODIFY)) {
                e.setCancelled(true);
            }
        }
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

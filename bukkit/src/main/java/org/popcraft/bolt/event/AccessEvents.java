package org.popcraft.bolt.event;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.util.defaults.DefaultPermission;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Optional;

public class AccessEvents implements Listener {
    private final BoltPlugin plugin;

    public AccessEvents(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Optional<BlockProtection> protection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
        final Player player = e.getPlayer();
        if (protection.isPresent()) {
            final BlockProtection blockProtection = protection.get();
            final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(player.getUniqueId());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, DefaultPermission.BREAK.getKey())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        final Optional<BlockProtection> protection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
        final Player player = e.getPlayer();
        if (protection.isPresent()) {
            final BlockProtection blockProtection = protection.get();
            final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(player.getUniqueId());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, DefaultPermission.INTERACT.getKey())) {
                e.setCancelled(true);
                if (EquipmentSlot.HAND.equals(e.getHand())) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Template.of("block", Strings.toTitleCase(block.getType())));
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
            final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(e.getPlayer().getUniqueId());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, DefaultPermission.INTERACT.getKey())) {
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
            if (!(e.getRemover() instanceof final Player player) || !plugin.getBolt().getAccessManager().hasAccess(plugin.getBolt().getPlayerMeta(player.getUniqueId()), entityProtection, DefaultPermission.KILL.getKey())) {
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
            if (!(e.getAttacker() instanceof final Player player) || !plugin.getBolt().getAccessManager().hasAccess(plugin.getBolt().getPlayerMeta(player.getUniqueId()), entityProtection, DefaultPermission.KILL.getKey())) {
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
            final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(e.getPlayer().getUniqueId());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, DefaultPermission.INTERACT.getKey())) {
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
            if (!(e.getDamager() instanceof final Player player) || !plugin.getBolt().getAccessManager().hasAccess(plugin.getBolt().getPlayerMeta(player.getUniqueId()), entityProtection, DefaultPermission.KILL.getKey())) {
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
            final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(e.getPlayer().getUniqueId());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, DefaultPermission.INTERACT.getKey())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(final InventoryOpenEvent e) {
        // TODO: Do this better
        if (!(e.getPlayer() instanceof Player player)) {
            return;
        }
        final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(player.getUniqueId());
        final InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder instanceof final BlockInventoryHolder blockInventoryHolder) {
            final Block block = blockInventoryHolder.getBlock();
            final Optional<BlockProtection> protection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
            if (protection.isPresent()) {
                final BlockProtection blockProtection = protection.get();
                if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, DefaultPermission.CONTAINER_ACCESS.getKey())) {
                    e.setCancelled(true);
                }
            }
        } else if (inventoryHolder instanceof final Entity entity) {
            final Optional<EntityProtection> protection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
            if (protection.isPresent()) {
                final EntityProtection entityProtection = protection.get();
                if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, DefaultPermission.CONTAINER_ACCESS.getKey())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        // TODO: Do this when the other inventory event is handled better (and this one will be pretty similar)
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        // TODO: Do this when the other inventory event is handled better (and this one will be pretty similar)
    }

    @EventHandler
    public void onPlayerTakeLecternBook(final PlayerTakeLecternBookEvent e) {
        final Block block = e.getLectern().getBlock();
        final Optional<BlockProtection> protection = plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(block));
        if (protection.isPresent()) {
            final BlockProtection blockProtection = protection.get();
            final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(e.getPlayer().getUniqueId());
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, blockProtection, DefaultPermission.CONTAINER_REMOVE.getKey())) {
                e.setCancelled(true);
            }
        }
    }
}

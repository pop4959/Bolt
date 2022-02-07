package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.Optional;
import java.util.UUID;

import static org.popcraft.bolt.util.lang.Translator.translate;

@SuppressWarnings("ClassCanBeRecord")
public final class EntityListener implements Listener {
    private final BoltPlugin plugin;

    public EntityListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent e) {
        final Entity entity = e.getEntity();
        plugin.findProtection(entity).ifPresent(protection -> {
            plugin.removeProtection(protection);
            if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent entityDamageByEntityEvent && getDamagerSource(entityDamageByEntityEvent.getDamager()) instanceof final Player player && plugin.canAccessProtection(player, protection, Permission.DESTROY)) {
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
            }
        });
    }

    @EventHandler
    public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        if (handlePlayerEntityInteraction(e.getPlayer(), e.getRightClicked(), Permission.INTERACT, EquipmentSlot.HAND.equals(e.getHand()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        final Entity damager = getDamagerSource(e.getDamager());
        final Entity entity = e.getEntity();
        if ((damager instanceof final Player player && handlePlayerEntityInteraction(player, entity, Permission.DESTROY, true)) || (!(damager instanceof Player) && plugin.findProtection(entity).isPresent())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleDamage(final VehicleDamageEvent e) {
        final Entity attacker = getDamagerSource(e.getAttacker());
        final Entity vehicle = e.getVehicle();
        if (attacker instanceof final Player player && handlePlayerEntityInteraction(player, vehicle, Permission.INTERACT, true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreakByEntity(final HangingBreakByEntityEvent e) {
        if (getDamagerSource(e.getRemover()) instanceof final Player player) {
            if (handlePlayerEntityInteraction(player, e.getEntity(), Permission.DESTROY, true)) {
                e.setCancelled(true);
            } else {
                plugin.findProtection(e.getEntity()).ifPresent(protection -> {
                    plugin.removeProtection(protection);
                    if (plugin.canAccessProtection(player, protection, Permission.DESTROY)) {
                        BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Strings.toTitleCase(e.getEntity().getType())));
                    }
                });
            }
        } else if (plugin.findProtection(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    private boolean handlePlayerEntityInteraction(final Player player, final Entity entity, final String permission, final boolean shouldSendMessage) {
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        if (playerMeta.hasInteracted()) {
            return true;
        }
        boolean shouldCancel = false;
        final Protection protection = plugin.findProtection(entity).orElse(null);
        if (triggerActions(player, protection, entity)) {
            playerMeta.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, playerMeta::clearInteraction);
            shouldCancel = true;
        } else if (protection != null) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            if (!plugin.canAccessProtection(player, protection, permission)) {
                shouldCancel = true;
                if (shouldSendMessage && !hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
            }
            if (shouldSendMessage && hasNotifyPermission) {
                BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY, Template.of("type", Strings.toTitleCase(entity.getType())), Template.of("owner", player.getUniqueId().equals(protection.getOwner()) ? translate(Translation.YOU) : BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN))));
            }
            playerMeta.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, playerMeta::clearInteraction);
        }
        return shouldCancel;
    }

    @SuppressWarnings("java:S6205")
    private boolean triggerActions(final Player player, final Protection protection, final Entity entity) {
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Action action = playerMeta.triggerAction();
        if (action == null) {
            return false;
        }
        switch (action) {
            case LOCK -> {
                if (protection != null) {
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY, Template.of("type", Strings.toTitleCase(entity.getType())));
                } else {
                    plugin.getBolt().getStore().saveEntityProtection(BukkitAdapter.createPrivateEntityProtection(entity, playerMeta.isLockNil() ? UUID.fromString("00000000-0000-0000-0000-000000000000") : player.getUniqueId()));
                    playerMeta.setLockNil(false);
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    plugin.removeProtection(protection);
                    BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
            }
            case INFO -> {
                if (protection != null) {
                    BoltComponents.sendMessage(player, Translation.INFO, Template.of("access", Strings.toTitleCase(protection.getType())), Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN))));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
            }
            case EDIT -> {
                if (protection != null) {
                    if (plugin.canAccessProtection(player, protection, Permission.EDIT)) {
                        playerMeta.getModifications().forEach((source, type) -> {
                            if (type == null || plugin.getBolt().getAccessRegistry().get(type).isEmpty()) {
                                protection.getAccess().remove(source);
                            } else {
                                protection.getAccess().put(source, type);
                            }
                        });
                        plugin.removeProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED, Template.of("type", Strings.toTitleCase(entity.getType())));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_PERMISSION);
                    }
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
                playerMeta.getModifications().clear();
            }
            case DEBUG -> BoltComponents.sendMessage(player, Optional.ofNullable(protection).map(Protection::toString).toString());
        }
        return true;
    }

    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent e) {
        final Optional<Protection> protection = plugin.findProtection(e.getVehicle());
        if (protection.isPresent() && (!(getDamagerSource(e.getAttacker()) instanceof final Player player) || !plugin.canAccessProtection(player, protection.get(), Permission.DESTROY))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof final ItemFrame itemFrame)) {
            return;
        }
        final Player player = e.getPlayer();
        if (plugin.playerMeta(player).triggeredAction() || !plugin.canAccessEntity(player, itemFrame, Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(final PlayerArmorStandManipulateEvent e) {
        if (!plugin.canAccessEntity(e.getPlayer(), e.getRightClicked(), Permission.MODIFY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreak(final HangingBreakEvent e) {
        if (HangingBreakEvent.RemoveCause.ENTITY.equals(e.getCause())) {
            return;
        }
        if (plugin.findProtection(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent e) {
        if (EntityDamageEvent.DamageCause.ENTITY_ATTACK.equals(e.getCause()) || EntityDamageEvent.DamageCause.PROJECTILE.equals(e.getCause())) {
            return;
        }
        if (plugin.findProtection(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityMount(final EntityMountEvent e) {
        if (!(e.getEntity() instanceof final Player player)) {
            return;
        }
        if (plugin.playerMeta(player).triggeredAction() || !plugin.canAccessEntity(player, e.getMount(), Permission.MOUNT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityEnterLoveMode(final EntityEnterLoveModeEvent e) {
        // TODO: Potentially look into a solution for items being lost when cancelling
        if (!(e.getHumanEntity() instanceof final Player player)) {
            return;
        }
        if (!plugin.canAccessEntity(player, e.getEntity(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSheepDyeWool(final SheepDyeWoolEvent e) {
        final Optional<Protection> protection = plugin.findProtection(e.getEntity());
        if (protection.isEmpty()) {
            return;
        }
        final Player player = e.getPlayer();
        if (player == null || !plugin.canAccessProtection(player, protection.get(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerShearEntity(final PlayerShearEntityEvent e) {
        if (!plugin.canAccessEntity(e.getPlayer(), e.getEntity(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLeashEntity(final PlayerLeashEntityEvent e) {
        if (!plugin.canAccessEntity(e.getPlayer(), e.getEntity(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerUnleashEntity(final PlayerUnleashEntityEvent e) {
        if (!plugin.canAccessEntity(e.getPlayer(), e.getEntity(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    private Entity getDamagerSource(final Entity damager) {
        if (damager instanceof final Projectile projectile && projectile.getShooter() instanceof final Entity source) {
            return source;
        } else {
            return damager;
        }
    }
}

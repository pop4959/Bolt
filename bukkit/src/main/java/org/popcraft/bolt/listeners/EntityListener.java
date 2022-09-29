package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.Objects;
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
            if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent entityDamageByEntityEvent && getDamagerSource(entityDamageByEntityEvent.getDamager()) instanceof final Player player && plugin.canAccess(protection, player, Permission.DESTROY)) {
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Placeholder.unparsed("type", Protections.displayType(protection)));
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
        final Entity entity = e.getEntity();
        if (getDamagerSource(e.getRemover()) instanceof final Player player) {
            if (handlePlayerEntityInteraction(player, entity, Permission.DESTROY, true)) {
                e.setCancelled(true);
            } else {
                plugin.findProtection(entity).ifPresent(protection -> {
                    plugin.removeProtection(protection);
                    if (plugin.canAccess(protection, player, Permission.DESTROY)) {
                        BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Placeholder.unparsed("type", Protections.displayType(protection)));
                    }
                });
            }
        } else if (plugin.findProtection(entity).isPresent()) {
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
            if (!plugin.canAccess(protection, player, permission)) {
                shouldCancel = true;
                if (shouldSendMessage && !hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Placeholder.unparsed("type", Protections.displayType(protection)));
                }
            }
            if (shouldSendMessage && hasNotifyPermission) {
                BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY, Placeholder.unparsed("type", Protections.displayType(protection)), Placeholder.unparsed("owner", player.getUniqueId().equals(protection.getOwner()) ? translate(Translation.YOU) : Objects.requireNonNullElse(plugin.getUuidCache().getName(protection.getOwner()), translate(Translation.UNKNOWN))));
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
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY, Placeholder.unparsed("type", Protections.displayType(protection)));
                } else {
                    plugin.getBolt().getStore().saveEntityProtection(BukkitAdapter.createPrivateEntityProtection(entity, playerMeta.isLockNil() ? UUID.fromString("00000000-0000-0000-0000-000000000000") : player.getUniqueId()));
                    playerMeta.setLockNil(false);
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED, Placeholder.unparsed("type", Protections.displayType(entity)));
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    plugin.removeProtection(protection);
                    BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Placeholder.unparsed("type", Protections.displayType(protection)));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Placeholder.unparsed("type", Protections.displayType(entity)));
                }
            }
            case INFO -> {
                if (protection != null) {
                    BoltComponents.sendMessage(player, Translation.INFO, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("owner", Objects.requireNonNullElse(plugin.getUuidCache().getName(protection.getOwner()), translate(Translation.UNKNOWN))));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Placeholder.unparsed("type", Protections.displayType(entity)));
                }
            }
            case EDIT -> {
                if (protection != null) {
                    if (plugin.canAccess(protection, player, Permission.EDIT)) {
                        playerMeta.getModifications().forEach((source, type) -> {
                            if (type == null || plugin.getBolt().getAccessRegistry().get(type).isEmpty()) {
                                protection.getAccess().remove(source);
                            } else {
                                protection.getAccess().put(source, type);
                            }
                        });
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED, Placeholder.unparsed("type", Protections.displayType(protection)));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_PERMISSION);
                    }
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Placeholder.unparsed("type", Protections.displayType(entity)));
                }
                playerMeta.getModifications().clear();
            }
            case DEBUG ->
                    BoltComponents.sendMessage(player, Optional.ofNullable(protection).map(Protection::toString).toString());
        }
        return true;
    }

    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent e) {
        final Entity vehicle = e.getVehicle();
        final Optional<Protection> optionalProtection = plugin.findProtection(vehicle);
        if (optionalProtection.isPresent()) {
            final Protection protection = optionalProtection.get();
            if (!(getDamagerSource(e.getAttacker()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)) {
                e.setCancelled(true);
            } else {
                plugin.removeProtection(protection);
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Placeholder.unparsed("type", Protections.displayType(protection)));
            }
        }
    }

    @EventHandler
    public void onVehicleEnter(final VehicleEnterEvent e) {
        plugin.findProtection(e.getEntered()).ifPresent(entityProtection -> {
            if (plugin.findProtection(e.getVehicle()).map(vehicleProtection -> !plugin.canAccess(vehicleProtection, entityProtection.getOwner(), Permission.MOUNT)).orElse(true)) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent e) {
        final Player player = e.getPlayer();
        if (plugin.playerMeta(player).triggeredAction()) {
            e.setCancelled(true);
            return;
        }
        if (!plugin.canAccess(e.getRightClicked(), player, Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(final PlayerArmorStandManipulateEvent e) {
        if (!plugin.canAccess(e.getRightClicked(), e.getPlayer(), Permission.INTERACT)) {
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
        if (EntityDamageEvent.DamageCause.ENTITY_ATTACK.equals(e.getCause()) || EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK.equals(e.getCause()) || EntityDamageEvent.DamageCause.PROJECTILE.equals(e.getCause()) || EntityDamageEvent.DamageCause.ENTITY_EXPLOSION.equals(e.getCause())) {
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
        if (plugin.playerMeta(player).triggeredAction() || !plugin.canAccess(e.getMount(), player, Permission.MOUNT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityEnterLoveMode(final EntityEnterLoveModeEvent e) {
        // TODO: Potentially look into a solution for items being lost when cancelling
        if (!(e.getHumanEntity() instanceof final Player player)) {
            return;
        }
        if (!plugin.canAccess(e.getEntity(), player, Permission.INTERACT)) {
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
        if (player == null || !plugin.canAccess(protection.get(), player, Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerShearEntity(final PlayerShearEntityEvent e) {
        if (!plugin.canAccess(e.getEntity(), e.getPlayer(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockShearEntity(final BlockShearEntityEvent e) {
        if (plugin.findProtection(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLeashEntity(final PlayerLeashEntityEvent e) {
        if (!plugin.canAccess(e.getEntity(), e.getPlayer(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerUnleashEntity(final PlayerUnleashEntityEvent e) {
        if (!plugin.canAccess(e.getEntity(), e.getPlayer(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTransform(final EntityTransformEvent e) {
        if (plugin.findProtection(e.getTransformedEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(final EntityTargetLivingEntityEvent e) {
        if (EntityTargetEvent.TargetReason.TEMPT.equals(e.getReason()) && e.getTarget() instanceof final Player player && !plugin.canAccess(e.getEntity(), player, Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombustByBlock(final EntityCombustByBlockEvent e) {
        if (plugin.findProtection(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombustByEntity(final EntityCombustByEntityEvent e) {
        plugin.findProtection(e.getEntity()).ifPresent(protection -> {
            if (!(getDamagerSource(e.getCombuster()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onPlayerBucketEntity(final PlayerBucketEntityEvent e) {
        plugin.findProtection(e.getEntity()).ifPresent(protection -> {
            if (!plugin.canAccess(protection, e.getPlayer(), Permission.DESTROY)) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onProjectileHit(final ProjectileHitEvent e) {
        final Entity hitEntity = e.getHitEntity();
        if (hitEntity == null) {
            return;
        }
        plugin.findProtection(hitEntity).ifPresent(protection -> {
            if (!(getDamagerSource(e.getEntity()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onPotionSplash(final PotionSplashEvent e) {
        e.getAffectedEntities().removeIf(livingEntity -> plugin.findProtection(livingEntity).map(protection -> !(getDamagerSource(e.getEntity()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)).orElse(false));
    }

    @EventHandler
    public void onAreaEffectCloudApply(final AreaEffectCloudApplyEvent e) {
        e.getAffectedEntities().removeIf(livingEntity -> plugin.findProtection(livingEntity).map(protection -> !(e.getEntity().getSource() instanceof final Entity entity) || !(getDamagerSource(entity) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)).orElse(false));
    }

    @EventHandler
    public void onExplosionPrime(final ExplosionPrimeEvent e) {
        if (plugin.findProtection(e.getEntity()).isPresent()) {
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

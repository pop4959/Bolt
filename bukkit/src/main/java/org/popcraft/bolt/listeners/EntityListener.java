package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockShearEntityEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
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
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceTypeResolver;
import org.popcraft.bolt.source.SourceTypes;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Mode;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.ProtectableConfig;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.popcraft.bolt.lang.Translator.translate;
import static org.popcraft.bolt.util.Profiles.NIL_UUID;

public final class EntityListener implements Listener {
    private static final SourceResolver ENTITY_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.ENTITY));
    private final Map<NamespacedKey, UUID> spawnEggPlayers = new HashMap<>();
    private final BoltPlugin plugin;

    public EntityListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (!org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK.equals(e.getAction())) {
            return;
        }
        final NamespacedKey spawnEggKey = e.getMaterial().getKey();
        if (!spawnEggKey.getKey().endsWith("_spawn_egg")) {
            return;
        }
        final NamespacedKey entityKey = NamespacedKey.minecraft(spawnEggKey.getKey().replace("_spawn_egg", ""));
        final Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();
        spawnEggPlayers.put(entityKey, uuid);
        SchedulerUtil.schedule(plugin, player, () -> spawnEggPlayers.remove(entityKey));
    }

    @EventHandler
    public void onCreatureSpawn(final CreatureSpawnEvent e) {
        if (!CreatureSpawnEvent.SpawnReason.SPAWNER_EGG.equals(e.getSpawnReason())) {
            return;
        }
        final Entity entity = e.getEntity();
        final UUID uuid = spawnEggPlayers.remove(entity.getType().getKey());
        if (uuid == null) {
            return;
        }
        final Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            handleEntityPlacementByPlayer(entity, player);
        }
    }

    @EventHandler
    public void onEntityPlace(final EntityPlaceEvent e) {
        final Player player = e.getPlayer();
        if (player != null) {
            handleEntityPlacementByPlayer(e.getEntity(), player);
        }
    }

    @EventHandler
    public void onHangingPlace(final HangingPlaceEvent e) {
        final Player player = e.getPlayer();
        if (player != null) {
            handleEntityPlacementByPlayer(e.getEntity(), player);
        }
    }

    private void handleEntityPlacementByPlayer(final Entity entity, final Player player) {
        if (plugin.player(player.getUniqueId()).hasMode(Mode.NOLOCK)) {
            return;
        }
        if (!plugin.isProtectable(entity)) {
            return;
        }
        final ProtectableConfig protectableConfig = plugin.getProtectableConfig(entity);
        if (protectableConfig == null) {
            return;
        }
        if (protectableConfig.autoProtectPermission() && !player.hasPermission("bolt.protection.autoprotect.%s".formatted(entity.getType().name().toLowerCase()))) {
            return;
        }
        final Access access = protectableConfig.defaultAccess();
        if (access == null) {
            return;
        }
        if (access.restricted() && !player.hasPermission("bolt.type.protection.%s".formatted(access.type()))) {
            return;
        }
        final EntityProtection newProtection = plugin.createProtection(entity, player.getUniqueId(), access.type());
        plugin.saveProtection(newProtection);
        if (!plugin.player(player.getUniqueId()).hasMode(Mode.NOSPAM)) {
            BoltComponents.sendMessage(
                    player,
                    Translation.CLICK_LOCKED,
                    plugin.isUseActionBar(),
                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(newProtection)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(entity))
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent e) {
        final Entity entity = e.getEntity();
        final Protection protection = plugin.findProtection(entity);
        if (protection == null) {
            return;
        }
        plugin.removeProtection(protection);
        if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent entityDamageByEntityEvent && getDamagerSource(entityDamageByEntityEvent.getDamager()) instanceof final Player player && plugin.canAccess(protection, player, Permission.DESTROY)) {
            BoltComponents.sendMessage(
                    player,
                    Translation.CLICK_UNLOCKED,
                    plugin.isUseActionBar(),
                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
            );
        }
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
        if ((damager instanceof final Player player && handlePlayerEntityInteraction(player, entity, Permission.DESTROY, true)) || (!(damager instanceof Player) && plugin.isProtected(entity))) {
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
                final Protection protection = plugin.findProtection(entity);
                if (protection == null) {
                    return;
                }
                plugin.removeProtection(protection);
                if (plugin.canAccess(protection, player, Permission.DESTROY)) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_UNLOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
                    );
                }
            }
        } else if (plugin.isProtected(entity)) {
            e.setCancelled(true);
        }
    }

    private boolean handlePlayerEntityInteraction(final Player player, final Entity entity, final String permission, final boolean shouldSendMessage) {
        final BoltPlayer boltPlayer = plugin.player(player);
        if (boltPlayer.hasInteracted()) {
            return true;
        }
        boolean shouldCancel = false;
        final Protection protection = plugin.findProtection(entity);
        if (triggerActions(player, protection, entity)) {
            boltPlayer.setInteracted(true);
            SchedulerUtil.schedule(plugin, player, boltPlayer::clearInteraction);
            shouldCancel = true;
        } else if (protection != null) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            final boolean canAccess = plugin.canAccess(protection, player, permission);
            final boolean canInteract = canAccess && Permission.INTERACT.equals(permission);
            if (canInteract && protection instanceof final EntityProtection entityProtection) {
                protection.setAccessed(System.currentTimeMillis());
                plugin.saveProtection(entityProtection);
            }
            if (!canAccess) {
                shouldCancel = true;
                if (shouldSendMessage && !hasNotifyPermission) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
                    );
                }
            }
            if (shouldSendMessage && hasNotifyPermission) {
                Profiles.findOrLookupProfileByUniqueId(protection.getOwner()).thenAccept(profile -> {
                    final boolean noSpam = plugin.player(player.getUniqueId()).hasMode(Mode.NOSPAM);
                    if (noSpam) {
                        return;
                    }
                    final boolean isYou = player.getUniqueId().equals(protection.getOwner());
                    final String owner = isYou ? translate(Translation.YOU) : profile.name();
                    if (owner == null) {
                        SchedulerUtil.schedule(plugin, player, () -> {
                            if (!plugin.isProtected(entity)) {
                                return;
                            }
                            BoltComponents.sendMessage(
                                    player,
                                    Translation.PROTECTION_NOTIFY_GENERIC,
                                    plugin.isUseActionBar(),
                                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
                            );
                        });
                    } else if (!isYou || player.hasPermission("bolt.protection.notify.self")) {
                        SchedulerUtil.schedule(plugin, player, () -> {
                            if (!plugin.isProtected(entity)) {
                                return;
                            }
                            BoltComponents.sendMessage(
                                    player,
                                    Translation.PROTECTION_NOTIFY,
                                    plugin.isUseActionBar(),
                                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection)),
                                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(owner))
                            );
                        });
                    }
                });
            }
            boltPlayer.setInteracted(shouldCancel);
            SchedulerUtil.schedule(plugin, player, boltPlayer::clearInteraction);
        }
        return shouldCancel;
    }

    private boolean triggerActions(final Player player, final Protection protection, final Entity entity) {
        final BoltPlayer boltPlayer = plugin.player(player);
        final Action action = boltPlayer.getAction();
        if (action == null) {
            return false;
        }
        final Action.Type actionType = action.getType();
        switch (actionType) {
            case LOCK -> {
                final String protectionType = Optional.ofNullable(action.getData())
                        .flatMap(type -> plugin.getBolt().getAccessRegistry().getProtectionByType(type))
                        .map(Access::type)
                        .orElse(plugin.getDefaultProtectionType());
                final ProtectableConfig protectableConfig = plugin.getProtectableConfig(entity);
                final boolean lockPermission = protectableConfig != null && protectableConfig.lockPermission();
                if (protection != null) {
                    if (!protection.getType().equals(protectionType) && plugin.canAccess(protection, player, Permission.EDIT)) {
                        protection.setType(protectionType);
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_LOCKED_CHANGED,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection))
                        );
                    } else {
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_LOCKED_ALREADY,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
                        );
                    }
                } else if (plugin.isProtectable(entity) && (!lockPermission || player.hasPermission("bolt.protection.lock.%s".formatted(entity.getType().name().toLowerCase())))) {
                    final EntityProtection newProtection = plugin.createProtection(entity, boltPlayer.isLockNil() ? NIL_UUID : player.getUniqueId(), protectionType);
                    plugin.saveProtection(newProtection);
                    boltPlayer.setLockNil(false);
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(newProtection)),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(entity))
                    );
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKABLE,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(entity))
                    );
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    if (player.hasPermission("bolt.mod") || plugin.canAccess(protection, player, Permission.DESTROY)) {
                        plugin.removeProtection(protection);
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_UNLOCKED,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                                Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
                        );
                    } else {
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_UNLOCKED_NO_PERMISSION,
                                plugin.isUseActionBar()
                        );
                    }
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(entity))
                    );
                }
            }
            case INFO -> {
                if (protection != null) {
                    Profiles.findOrLookupProfileByUniqueId(protection.getOwner())
                            .thenAccept(profile -> SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                    player,
                                    protection.getAccess().size() > 0 && (protection.getOwner().equals(player.getUniqueId()) || player.hasPermission("bolt.command.info.full")) ? Translation.INFO_FULL : Translation.INFO,
                                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection)),
                                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(Optional.ofNullable(profile.name()).orElse(translate(Translation.UNKNOWN)))),
                                    Placeholder.component(Translation.Placeholder.ACCESS_LIST_SIZE, Component.text(protection.getAccess().size())),
                                    Placeholder.component(Translation.Placeholder.ACCESS_LIST, Component.text(Protections.accessList(protection.getAccess())))
                            )));
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(entity))
                    );
                }
            }
            case EDIT -> {
                if (protection != null) {
                    if (plugin.canAccess(protection, player, Permission.EDIT)) {
                        boltPlayer.consumeModifications().forEach((source, type) -> {
                            if (Boolean.parseBoolean(action.getData())) {
                                protection.getAccess().put(source.toString(), type);
                            } else {
                                protection.getAccess().remove(source.toString());
                            }
                        });
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_EDITED,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                                Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
                        );
                    } else {
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_EDITED_NO_PERMISSION,
                                plugin.isUseActionBar()
                        );
                    }
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(entity))
                    );
                }
            }
            case DEBUG -> BoltComponents.sendMessage(
                    player,
                    Optional.ofNullable(protection).map(Protection::toString).toString()
            );
            case TRANSFER -> {
                if (protection != null) {
                    if (player.getUniqueId().equals(protection.getOwner()) || action.isAdmin()) {
                        final UUID uuid = UUID.fromString(action.getData());
                        protection.setOwner(uuid);
                        plugin.saveProtection(protection);
                        Profiles.findOrLookupProfileByUniqueId(uuid)
                                .thenAccept(profile -> SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                        player,
                                        Translation.CLICK_TRANSFER_CONFIRM,
                                        plugin.isUseActionBar(),
                                        Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection)),
                                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(Optional.ofNullable(profile.name()).orElse(translate(Translation.UNKNOWN))))
                                )));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_OWNER, plugin.isUseActionBar());
                    }
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(entity))
                    );
                }
            }
        }
        boltPlayer.clearAction();
        return true;
    }

    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent e) {
        final Entity vehicle = e.getVehicle();
        final Protection protection = plugin.findProtection(vehicle);
        if (protection == null) {
            return;
        }
        if (!(getDamagerSource(e.getAttacker()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)) {
            e.setCancelled(true);
        } else {
            plugin.removeProtection(protection);
            BoltComponents.sendMessage(
                    player,
                    Translation.CLICK_UNLOCKED,
                    plugin.isUseActionBar(),
                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection))
            );
        }
    }

    @EventHandler
    public void onVehicleEnter(final VehicleEnterEvent e) {
        final Protection protection = plugin.findProtection(e.getEntered());
        if (protection == null) {
            return;
        }
        if (!plugin.canAccess(protection, e.getEntered().getUniqueId(), Permission.MOUNT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent e) {
        final Player player = e.getPlayer();
        if (plugin.player(player).triggeredAction()) {
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
        if (plugin.isProtected(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent e) {
        if (EntityDamageEvent.DamageCause.ENTITY_ATTACK.equals(e.getCause()) || EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK.equals(e.getCause()) || EntityDamageEvent.DamageCause.PROJECTILE.equals(e.getCause()) || EntityDamageEvent.DamageCause.ENTITY_EXPLOSION.equals(e.getCause())) {
            return;
        }
        if (plugin.isProtected(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityMount(final EntityMountEvent e) {
        if (!(e.getEntity() instanceof final Player player)) {
            return;
        }
        if (plugin.player(player).triggeredAction() || !plugin.canAccess(e.getMount(), player, Permission.MOUNT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSheepDyeWool(final SheepDyeWoolEvent e) {
        final Protection protection = plugin.findProtection(e.getEntity());
        if (protection == null) {
            return;
        }
        final Player player = e.getPlayer();
        if (player == null || !plugin.canAccess(protection, player, Permission.INTERACT)) {
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
        if (plugin.isProtected(e.getEntity())) {
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
        if (plugin.isProtected(e.getTransformedEntity())) {
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
        if (plugin.isProtected(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityCombustByEntity(final EntityCombustByEntityEvent e) {
        final Protection protection = plugin.findProtection(e.getEntity());
        if (protection == null) {
            return;
        }
        if (!(getDamagerSource(e.getCombuster()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketEntity(final PlayerBucketEntityEvent e) {
        final Protection protection = plugin.findProtection(e.getEntity());
        if (protection == null) {
            return;
        }
        if (!plugin.canAccess(protection, e.getPlayer(), Permission.DESTROY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectileHit(final ProjectileHitEvent e) {
        final Entity hitEntity = e.getHitEntity();
        if (hitEntity == null) {
            return;
        }
        final Protection protection = plugin.findProtection(hitEntity);
        if (protection == null) {
            return;
        }
        if (!(getDamagerSource(e.getEntity()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPotionSplash(final PotionSplashEvent e) {
        e.getAffectedEntities().removeIf(livingEntity -> {
            final Protection protection = plugin.findProtection(livingEntity);
            if (protection == null) {
                return false;
            }
            return !(getDamagerSource(e.getEntity()) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY);
        });
    }

    @EventHandler
    public void onAreaEffectCloudApply(final AreaEffectCloudApplyEvent e) {
        e.getAffectedEntities().removeIf(livingEntity -> {
            final Protection protection = plugin.findProtection(livingEntity);
            if (protection == null) {
                return false;
            }
            return !(e.getEntity().getSource() instanceof final Entity entity) || !(getDamagerSource(entity) instanceof final Player player) || !plugin.canAccess(protection, player, Permission.DESTROY);
        });
    }

    @EventHandler
    public void onExplosionPrime(final ExplosionPrimeEvent e) {
        if (plugin.isProtected(e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void entityInteract(final EntityInteractEvent e) {
        if (e.getEntity() instanceof Player) {
            return;
        }
        final Protection protection = plugin.findProtection(e.getBlock());
        if (protection == null) {
            return;
        }
        if (!plugin.canAccess(protection, ENTITY_SOURCE_RESOLVER, Permission.ENTITY_INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBreakDoor(final EntityBreakDoorEvent e) {
        if (e.getEntity() instanceof Player) {
            return;
        }
        final Protection protection = plugin.findProtection(e.getBlock());
        if (protection == null) {
            return;
        }
        if (!plugin.canAccess(protection, ENTITY_SOURCE_RESOLVER, Permission.ENTITY_BREAK_DOOR)) {
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

package org.popcraft.bolt.event;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.popcraft.bolt.Bolt;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.protection.EntityProtection;
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

import static org.popcraft.bolt.util.lang.Translator.translate;

@SuppressWarnings("ClassCanBeRecord")
public class EntityListener implements Listener {
    private final BoltPlugin plugin;

    public EntityListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent e) {
        final Entity entity = e.getEntity();
        final Store store = plugin.getBolt().getStore();
        // TODO: Figure out player in damage event to print unlock message
        store.loadEntityProtection(entity.getUniqueId()).ifPresent(store::removeEntityProtection);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        if (handlePlayerEntityInteraction(e.getPlayer(), e.getRightClicked(), Permission.INTERACT, EquipmentSlot.HAND.equals(e.getHand()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        final Entity damager = e.getDamager();
        final Entity entity = e.getEntity();
        if ((damager instanceof final Player player && handlePlayerEntityInteraction(player, entity, Permission.KILL, true)) || (!(damager instanceof Player) && plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId()).isPresent())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleDamage(final VehicleDamageEvent e) {
        final Entity attacker = e.getAttacker();
        final Entity vehicle = e.getVehicle();
        if (attacker instanceof final Player player && handlePlayerEntityInteraction(player, vehicle, Permission.INTERACT, true)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingBreakByEntity(final HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof final Player player && handlePlayerEntityInteraction(player, e.getEntity(), Permission.BREAK, true)) {
            e.setCancelled(true);
        }
    }

    private boolean handlePlayerEntityInteraction(final Player player, final Entity entity, final String permission, final boolean shouldSendMessage) {
        boolean shouldCancel = false;
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Bolt bolt = plugin.getBolt();
        final Store store = bolt.getStore();
        final Optional<EntityProtection> optionalProtection = store.loadEntityProtection(entity.getUniqueId());
        if (playerMeta.triggerAction(Action.LOCK)) {
            if (optionalProtection.isPresent()) {
                if (shouldSendMessage) {
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY,
                            Template.of("type", Strings.toTitleCase(entity.getType()))
                    );
                }
            } else {
                store.saveEntityProtection(BukkitAdapter.createPrivateEntityProtection(entity, player));
                if (shouldSendMessage) {
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED,
                            Template.of("type", Strings.toTitleCase(entity.getType()))
                    );
                }
            }
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.UNLOCK)) {
            if (optionalProtection.isPresent()) {
                store.removeEntityProtection(optionalProtection.get());
                if (shouldSendMessage) {
                    BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED,
                            Template.of("type", Strings.toTitleCase(entity.getType()))
                    );
                }
            } else {
                if (shouldSendMessage) {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED,
                            Template.of("type", Strings.toTitleCase(entity.getType()))
                    );
                }
            }
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.INFO)) {
            optionalProtection.ifPresentOrElse(protection -> {
                        if (shouldSendMessage) {
                            BoltComponents.sendMessage(player, Translation.INFO,
                                    Template.of("access", Strings.toTitleCase(protection.getType())),
                                    Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
                            );
                        }
                    }, () -> {
                        if (shouldSendMessage) {
                            BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                        }
                    }
            );
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.MODIFY)) {
            optionalProtection.ifPresentOrElse(protection -> {
                playerMeta.getModifications().forEach((source, type) -> {
                    if (type == null || bolt.getAccessRegistry().get(type).isEmpty()) {
                        protection.getAccess().remove(source);
                    } else {
                        protection.getAccess().put(source, type);
                    }
                });
                bolt.getStore().saveEntityProtection(protection);
                if (shouldSendMessage) {
                    BoltComponents.sendMessage(player, Translation.CLICK_MODIFIED);
                }
            }, () -> {
                if (shouldSendMessage) {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
            });
            playerMeta.getModifications().clear();
            shouldCancel = true;
        } else if (playerMeta.triggerAction(Action.DEBUG)) {
            if (shouldSendMessage) {
                BoltComponents.sendMessage(player, optionalProtection.map(Protection::toString).toString());
            }
            shouldCancel = true;
        } else if (optionalProtection.isPresent()) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            final EntityProtection protection = optionalProtection.get();
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, protection, permission)) {
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
    public void onPlayerInteractEntity(final PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof final ItemFrame itemFrame)) {
            return;
        }
        if (itemFrame.getItem().getType().isAir()) {
            return;
        }
        final Player player = e.getPlayer();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        plugin.getBolt().getStore().loadEntityProtection(itemFrame.getUniqueId()).ifPresent(entityProtection -> {
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.INTERACT)) {
                e.setCancelled(true);
            }
        });
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
    public void onHangingBreak(final HangingBreakEvent e) {
        if (HangingBreakEvent.RemoveCause.ENTITY.equals(e.getCause())) {
            return;
        }
        final Entity entity = e.getEntity();
        if (plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent e) {
        if (EntityDamageEvent.DamageCause.ENTITY_ATTACK.equals(e.getCause())) {
            return;
        }
        final Entity entity = e.getEntity();
        if (plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityMount(final EntityMountEvent e) {
        if (!(e.getEntity() instanceof final Player player)) {
            return;
        }
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Entity mount = e.getMount();
        plugin.getBolt().getStore().loadEntityProtection(mount.getUniqueId()).ifPresent(entityProtection -> {
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.MOUNT)) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onEntityEnterLoveMode(final EntityEnterLoveModeEvent e) {
        // TODO: Potentially look into a solution for items being lost when cancelling
        if (!(e.getHumanEntity() instanceof final Player player)) {
            return;
        }
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Animals entity = e.getEntity();
        plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId()).ifPresent(entityProtection -> {
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.INTERACT)) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onSheepDyeWool(final SheepDyeWoolEvent e) {
        final Entity entity = e.getEntity();
        final Optional<EntityProtection> optionalEntityProtection = plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId());
        final Player player = e.getPlayer();
        if (player == null && optionalEntityProtection.isPresent()) {
            e.setCancelled(true);
        } else if (player != null) {
            final PlayerMeta playerMeta = plugin.playerMeta(player);
            optionalEntityProtection.ifPresent(entityProtection -> {
                if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.INTERACT)) {
                    e.setCancelled(true);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerShearEntity(final PlayerShearEntityEvent e) {
        final Player player = e.getPlayer();
        final Entity entity = e.getEntity();
        plugin.getBolt().getStore().loadEntityProtection(entity.getUniqueId()).ifPresent(entityProtection -> {
            final PlayerMeta playerMeta = plugin.playerMeta(player);
            if (!plugin.getBolt().getAccessManager().hasAccess(playerMeta, entityProtection, Permission.INTERACT)) {
                e.setCancelled(true);
            }
        });
    }
}

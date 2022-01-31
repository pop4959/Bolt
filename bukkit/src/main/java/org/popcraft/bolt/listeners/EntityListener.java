package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.minimessage.Template;
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
public final class EntityListener implements Listener {
    private final BoltPlugin plugin;

    public EntityListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(final EntityDeathEvent e) {
        // TODO: Figure out player in damage event to print unlock message
        plugin.getEntityProtection(e.getEntity()).ifPresent(protection -> plugin.getBolt().getStore().removeEntityProtection(protection));
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
        if ((damager instanceof final Player player && handlePlayerEntityInteraction(player, entity, Permission.KILL, true)) || (!(damager instanceof Player) && plugin.getEntityProtection(entity).isPresent())) {
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
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        if (playerMeta.hasInteracted()) {
            return true;
        }
        boolean shouldCancel = false;
        final EntityProtection protection = plugin.getEntityProtection(entity).orElse(null);
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
    private boolean triggerActions(final Player player, final EntityProtection protection, final Entity entity) {
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
                    plugin.getBolt().getStore().saveEntityProtection(BukkitAdapter.createPrivateEntityProtection(entity, player));
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED, Template.of("type", Strings.toTitleCase(entity.getType())));
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    plugin.getBolt().getStore().removeEntityProtection(protection);
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
            case MODIFY -> {
                if (protection != null) {
                    playerMeta.getModifications().forEach((source, type) -> {
                        if (type == null || plugin.getBolt().getAccessRegistry().get(type).isEmpty()) {
                            protection.getAccess().remove(source);
                        } else {
                            protection.getAccess().put(source, type);
                        }
                    });
                    plugin.getBolt().getStore().saveEntityProtection(protection);
                    BoltComponents.sendMessage(player, Translation.CLICK_MODIFIED);
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
        final Optional<EntityProtection> protection = plugin.getEntityProtection(e.getVehicle());
        if (protection.isPresent() && (!(e.getAttacker() instanceof final Player player) || !plugin.canAccessProtection(player, protection.get(), Permission.KILL))) {
            e.setCancelled(true);
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
        if (!plugin.canAccessEntity(e.getPlayer(), itemFrame, Permission.INTERACT)) {
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
        if (plugin.getEntityProtection(e.getEntity()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent e) {
        if (EntityDamageEvent.DamageCause.ENTITY_ATTACK.equals(e.getCause())) {
            return;
        }
        if (plugin.getEntityProtection(e.getEntity()).isPresent()) {
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
        final Optional<EntityProtection> protection = plugin.getEntityProtection(e.getEntity());
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
}

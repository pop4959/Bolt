package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.event.Cancellable;
import org.popcraft.bolt.event.LockBlockEvent;
import org.popcraft.bolt.event.LockEntityEvent;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.ProtectableConfig;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;
import org.popcraft.bolt.util.Time;

import java.util.Optional;
import java.util.UUID;

import static org.popcraft.bolt.util.BoltComponents.resolveTranslation;
import static org.popcraft.bolt.util.Profiles.NIL_UUID;

abstract class InteractionListener {
    protected final BoltPlugin plugin;

    protected InteractionListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    protected boolean triggerAction(final Player player, final Protection protection, final Block block) {
        final boolean protectable = plugin.isProtectable(block);
        final ProtectableConfig config = plugin.getProtectableConfig(block);
        final Component displayName = Protections.displayType(block, player);
        final String lockPermission = "bolt.protection.lock.%s".formatted(block.getType().name().toLowerCase());
        return triggerAction(player, protection, block, protectable, config, displayName, lockPermission);
    }

    protected boolean triggerAction(final Player player, final Protection protection, final Entity entity) {
        final boolean protectable = plugin.isProtectable(entity);
        final ProtectableConfig config = plugin.getProtectableConfig(entity);
        final Component displayName = Protections.displayType(entity, player);
        final String lockPermission = "bolt.protection.lock.%s".formatted(entity.getType().name().toLowerCase());
        return triggerAction(player, protection, entity, protectable, config, displayName, lockPermission);
    }

    private boolean triggerAction(final Player player, final Protection protection, final Object object, final boolean protectable, final ProtectableConfig config, final Component displayName, final String lockPermission) {
        final BoltPlayer boltPlayer = plugin.player(player);
        final Action action = boltPlayer.getAction();
        if (action == null) {
            return false;
        }
        if (!player.hasPermission(action.getPermission())) {
            BoltComponents.sendMessage(player, Translation.COMMAND_NO_PERMISSION);
            return false;
        }
        final Action.Type actionType = action.getType();
        switch (actionType) {
            case LOCK -> {
                final Cancellable event;
                if (object instanceof final Block block) {
                    event = new LockBlockEvent(player, block, false);
                } else if (object instanceof final Entity entity) {
                    event = new LockEntityEvent(player, entity, false);
                } else {
                    throw new IllegalStateException("Protection is not a block or entity");
                }
                plugin.getEventBus().post(event);
                if (event.isCancelled()) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_LOCKED_CANCELLED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                    break;
                }
                final String protectionType = Optional.ofNullable(action.getData())
                        .flatMap(type -> plugin.getBolt().getAccessRegistry().getProtectionByType(type))
                        .map(Access::type)
                        .orElse(plugin.getDefaultProtectionType());
                final boolean requiresLockPermission = config != null && config.lockPermission();
                if (protection != null) {
                    if (!protection.getType().equals(protectionType) && plugin.canAccess(protection, player, Permission.EDIT)) {
                        protection.setType(protectionType);
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_LOCKED_CHANGED,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player))
                        );
                    } else {
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_LOCKED_ALREADY,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                        );
                    }
                } else if ((protectable || action.isAdmin()) && (!requiresLockPermission || player.hasPermission(lockPermission))) {
                    final UUID protectionUUID = boltPlayer.isLockNil() ? NIL_UUID : player.getUniqueId();
                    final Protection newProtection;
                    if (object instanceof final Block block) {
                        newProtection = plugin.createProtection(block, protectionUUID, protectionType);
                    } else if (object instanceof final Entity entity) {
                        newProtection = plugin.createProtection(entity, protectionUUID, protectionType);
                    } else {
                        throw new IllegalStateException("Protection is not a block or entity");
                    }
                    plugin.saveProtection(newProtection);
                    boltPlayer.setLockNil(false);
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(newProtection, player)),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKABLE,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    if (plugin.canAccess(protection, player, Permission.DESTROY)) {
                        plugin.removeProtection(protection);
                        BoltComponents.sendMessage(
                                player,
                                Translation.CLICK_UNLOCKED,
                                plugin.isUseActionBar(),
                                Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player)),
                                Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
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
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                }
            }
            case INFO -> {
                if (protection != null) {
                    final boolean showFull = protection.getOwner().equals(player.getUniqueId()) || player.hasPermission("bolt.command.info.full");
                    final boolean showAccessList = !protection.getAccess().isEmpty();
                    Profiles.findOrLookupProfileByUniqueId(protection.getOwner())
                            .thenAccept(profile -> SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                    player,
                                    showFull ? (showAccessList ? Translation.INFO_FULL_ACCESS : Translation.INFO_FULL_NO_ACCESS) : Translation.INFO,
                                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player)),
                                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player)),
                                    Placeholder.component(Translation.Placeholder.PLAYER, Optional.ofNullable(profile.name()).<Component>map(Component::text).orElse(resolveTranslation(Translation.UNKNOWN, player))),
                                    Placeholder.component(Translation.Placeholder.ACCESS_LIST_SIZE, Component.text(protection.getAccess().size())),
                                    Placeholder.component(Translation.Placeholder.ACCESS_LIST, Protections.accessList(protection.getAccess(), player)),
                                    Placeholder.component(Translation.Placeholder.CREATED_TIME, Time.relativeTimestamp(protection.getCreated(), player)),
                                    Placeholder.component(Translation.Placeholder.ACCESSED_TIME, Time.relativeTimestamp(protection.getAccessed(), player))
                            )));
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
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
                                Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player)),
                                Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
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
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
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
                                        Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player)),
                                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player)),
                                        Placeholder.component(Translation.Placeholder.PLAYER, Optional.ofNullable(profile.name()).<Component>map(Component::text).orElse(resolveTranslation(Translation.UNKNOWN, player)))
                                )));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_OWNER, plugin.isUseActionBar());
                    }
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_NOT_LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, displayName)
                    );
                }
            }
        }
        boltPlayer.clearAction();
        return true;
    }
}

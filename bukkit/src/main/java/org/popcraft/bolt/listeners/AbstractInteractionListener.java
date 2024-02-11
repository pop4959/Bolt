package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.Protectable;
import org.popcraft.bolt.util.ProtectableConfig;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;
import org.popcraft.bolt.util.Time;

import java.util.Optional;
import java.util.UUID;

import static org.popcraft.bolt.util.BoltComponents.resolveTranslation;
import static org.popcraft.bolt.util.Profiles.NIL_UUID;

abstract class AbstractInteractionListener<T extends Protectable> {
    protected final BoltPlugin plugin;

    protected AbstractInteractionListener(BoltPlugin plugin) {
        this.plugin = plugin;
    }

    protected boolean triggerActions(final Player player, final Protection protection, final T target) {
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
                final String protectionType = Optional.ofNullable(action.getData())
                    .flatMap(type -> plugin.getBolt().getAccessRegistry().getProtectionByType(type))
                    .map(Access::type)
                    .orElse(plugin.getDefaultProtectionType());
                final ProtectableConfig protectableConfig = plugin.getProtectableConfig(target);
                final boolean lockPermission = protectableConfig != null && protectableConfig.lockPermission();
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
                } else if (plugin.isProtectable(target) && (!lockPermission || player.hasPermission("bolt.protection.lock.%s".formatted(target.getTypeName().toLowerCase())))) {
                    final Protection newProtection = target.createProtection(boltPlayer.isLockNil() ? NIL_UUID : player.getUniqueId(), protectionType);
                    plugin.saveProtection(newProtection);
                    boltPlayer.setLockNil(false);
                    BoltComponents.sendMessage(
                        player,
                        Translation.CLICK_LOCKED,
                        plugin.isUseActionBar(),
                        Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(newProtection, player)),
                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(target, player))
                    );
                } else {
                    BoltComponents.sendMessage(
                        player,
                        Translation.CLICK_NOT_LOCKABLE,
                        plugin.isUseActionBar(),
                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(target, player))
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
                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(target, player))
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
                            Placeholder.component(Translation.Placeholder.ACCESS_LIST, Component.text(Protections.accessList(protection.getAccess()))),
                            Placeholder.component(Translation.Placeholder.CREATED_TIME, Time.relativeTimestamp(protection.getCreated(), player)),
                            Placeholder.component(Translation.Placeholder.ACCESSED_TIME, Time.relativeTimestamp(protection.getAccessed(), player))
                        )));
                } else {
                    BoltComponents.sendMessage(
                        player,
                        Translation.CLICK_NOT_LOCKED,
                        plugin.isUseActionBar(),
                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(target, player))
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
                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(target, player))
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
                        Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(target, player))
                    );
                }
            }
        }
        boltPlayer.clearAction();
        return true;
    }
}

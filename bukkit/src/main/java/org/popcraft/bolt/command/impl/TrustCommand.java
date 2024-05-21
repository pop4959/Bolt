package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.access.AccessList;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceType;
import org.popcraft.bolt.source.SourceTypes;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TrustCommand extends BoltCommand {
    public TrustCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        final String action = arguments.next();
        if ("add".equalsIgnoreCase(action) || "remove".equalsIgnoreCase(action)) {
            if (arguments.remaining() < 2) {
                shortHelp(sender, arguments);
                return;
            }
            final boolean adding = "add".equalsIgnoreCase(action);
            final String sourceTypeName = arguments.next().toLowerCase();
            final SourceType sourceType = plugin.getBolt().getSourceTypeRegistry().getSourceByName(sourceTypeName).orElse(null);
            if (sourceType == null || !plugin.getBolt().getSourceTypeRegistry().sourceTypes().contains(sourceType)) {
                BoltComponents.sendMessage(
                        sender,
                        Translation.EDIT_SOURCE_INVALID,
                        Placeholder.component(Translation.Placeholder.SOURCE_TYPE, Component.text(sourceTypeName))
                );
                return;
            }
            if (sourceType.restricted() && !sender.hasPermission("bolt.type.source.%s".formatted(sourceType.name()))) {
                BoltComponents.sendMessage(sender, Translation.EDIT_SOURCE_NO_PERMISSION);
                return;
            }
            final String sourceIdentifier = arguments.next();
            final String accessType = Objects.requireNonNullElse(arguments.next(), plugin.getDefaultAccessType()).toLowerCase();
            final Access access = plugin.getBolt().getAccessRegistry().getAccessByType(accessType).orElse(null);
            if (access == null) {
                BoltComponents.sendMessage(
                        sender,
                        Translation.EDIT_ACCESS_INVALID,
                        Placeholder.component(Translation.Placeholder.ACCESS_TYPE, Component.text(accessType))
                );
                return;
            }
            if (access.restricted() && !sender.hasPermission("bolt.type.access.%s".formatted(access.type()))) {
                BoltComponents.sendMessage(sender, Translation.EDIT_ACCESS_NO_PERMISSION);
                return;
            }
            final UUID uuid = player.getUniqueId();
            final AccessList accessList = Objects.requireNonNullElse(plugin.getBolt().getStore().loadAccessList(uuid).join(), new AccessList(uuid, new HashMap<>()));
            final CompletableFuture<Source> editFuture;
            if (SourceTypes.PLAYER.equals(sourceType.name())) {
                editFuture = Profiles.findOrLookupProfileByName(sourceIdentifier).thenApply(profile -> {
                    if (profile.uuid() != null) {
                        return Source.player(profile.uuid());
                    } else {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.PLAYER_NOT_FOUND,
                                Placeholder.component(Translation.Placeholder.PLAYER, Component.text(sourceIdentifier))
                        ));
                        return null;
                    }
                });
            } else if (SourceTypes.PASSWORD.equals(sourceType.name())) {
                editFuture = CompletableFuture.completedFuture(Source.password(sourceIdentifier));
            } else {
                editFuture = CompletableFuture.completedFuture(Source.of(sourceType.name(), sourceIdentifier));
            }
            editFuture.thenAccept(source -> SchedulerUtil.schedule(plugin, player, () -> {
                if (source != null) {
                    if (adding) {
                        accessList.getAccess().put(source.toString(), access.type());
                    } else {
                        accessList.getAccess().remove(source.toString());
                    }
                    plugin.getBolt().getStore().saveAccessList(accessList);
                    BoltComponents.sendMessage(sender, Translation.TRUST_EDITED);
                }
            }));
        } else {
            final AccessList accessList = plugin.getBolt().getStore().loadAccessList(player.getUniqueId()).join();
            final Map<String, String> accessMap = accessList == null ? new HashMap<>() : accessList.getAccess();
            BoltComponents.sendMessage(
                    player,
                    Translation.INFO_SELF,
                    Placeholder.component(Translation.Placeholder.ACCESS_LIST_SIZE, Component.text(accessMap.size())),
                    Placeholder.component(Translation.Placeholder.ACCESS_LIST, Protections.accessList(accessMap, sender))
            );
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        final String subcommand = arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("add", "remove");
        }
        final String sourceType = arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getBolt().getSourceTypeRegistry().sourceTypes().stream()
                    .filter(type -> !type.restricted() || sender.hasPermission("bolt.type.source.%s".formatted(type.name())))
                    .map(SourceType::name)
                    .toList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            if (SourceTypes.PLAYER.equals(sourceType)) {
                return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
            } else if (SourceTypes.GROUP.equals(sourceType) && sender instanceof final Player player) {
                return plugin.getPlayersOwnedGroups(player);
            } else {
                return Collections.emptyList();
            }
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getBolt().getAccessRegistry().access().stream()
                    .filter(access -> !access.restricted() || sender.hasPermission("bolt.type.access.%s".formatted(access.type())))
                    .map(Access::type)
                    .toList();
        }
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_TRUST,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt trust")),
                Placeholder.component(Translation.Placeholder.LITERAL, Component.text("(add|remove)"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_TRUST);
    }
}

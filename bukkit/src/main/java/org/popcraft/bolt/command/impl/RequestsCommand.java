package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Pagination;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.TransferRequests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RequestsCommand extends BoltCommand {
    public RequestsCommand(BoltPlugin plugin) {
        super(plugin);
    }

    private void processRequests(final Player player, final String action, final String name) {
        final TransferRequests transferRequests = plugin.getTransferRequests();
        final UUID uuid = player.getUniqueId();
        final List<UUID> players = "cancel".equals(action) ? transferRequests.recipients(uuid) : transferRequests.givers(uuid);
        for (final UUID other : players) {
            final String otherName = Profiles.findProfileByUniqueId(other).name();
            if (otherName == null || !otherName.equalsIgnoreCase(name)) {
                continue;
            }
            final int count = switch (action) {
                case "accept" -> transferRequests.accept(uuid, other);
                case "deny" -> transferRequests.deny(uuid, other);
                default -> transferRequests.cancel(uuid, other);
            };
            if (count == 0) {
                break;
            }
            final String message = switch (action) {
                case "accept" -> Translation.REQUESTS_ACCEPTED;
                case "deny" -> Translation.REQUESTS_DENIED;
                default -> Translation.REQUESTS_CANCELLED;
            };
            BoltComponents.sendMessage(
                    player,
                    message,
                    Placeholder.component(Translation.Placeholder.COUNT, Component.text(count)),
                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(otherName))
            );
            return;
        }
        BoltComponents.sendMessage(player, Translation.REQUESTS_NONE);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        if (arguments.remaining() < 1) {
            shortHelp(sender, arguments);
            return;
        }
        final String action = arguments.next().toLowerCase();
        switch (action) {
            case "list" -> Pagination.runPage(plugin, player, plugin.getTransferRequests().incoming(player.getUniqueId()), plugin.getTransferRequests().outgoing(player.getUniqueId()), 0);
            case "accept", "deny", "cancel" -> {
                final String name = arguments.next();
                if (name == null) {
                    shortHelp(sender, arguments);
                } else {
                    processRequests(player, action, name);
                }
            }
            default -> shortHelp(sender, arguments);
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player) || arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        final List<UUID> givers = plugin.getTransferRequests().givers(player.getUniqueId());
        final List<UUID> recipients = plugin.getTransferRequests().recipients(player.getUniqueId());
        final String action = arguments.next().toLowerCase();
        if (arguments.remaining() == 0) {
            final List<String> actions = new ArrayList<>();
            if (!givers.isEmpty() || !recipients.isEmpty()) {
                actions.add("list");
            }
            if (!givers.isEmpty()) {
                actions.add("accept");
                actions.add("deny");
            }
            if (!recipients.isEmpty()) {
                actions.add("cancel");
            }
            return actions;
        }
        arguments.next();
        if (arguments.remaining() != 0) {
            return Collections.emptyList();
        }
        final List<UUID> players = switch (action) {
            case "accept", "deny" -> givers;
            case "cancel" -> recipients;
            default -> Collections.emptyList();
        };
        return players.stream().map(uuid -> Profiles.findProfileByUniqueId(uuid).name()).filter(Objects::nonNull).toList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_REQUESTS,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt requests")),
                Placeholder.component(Translation.Placeholder.LITERAL, Component.text("(list|accept|deny|cancel)"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_REQUESTS);
    }

    @Override
    public boolean hidden(CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return true;
        }
        return !plugin.getTransferRequests().hasAny(player.getUniqueId());
    }
}

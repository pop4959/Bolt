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

public class AdminTrustCommand extends TrustCommand {
    public AdminTrustCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final String target = arguments.next();
        Profiles.findOrLookupProfileByName(target).thenAccept(profile -> {
            if (profile.uuid() != null) {
                final String action = arguments.next();
                if ("add".equalsIgnoreCase(action) || "remove".equalsIgnoreCase(action)) {
                    if (arguments.remaining() < 2) {
                        shortHelp(sender, arguments);
                        return;
                    }
                    final boolean adding = "add".equalsIgnoreCase(action);
                    super.trustModify(sender, profile.uuid(), adding, arguments);
                } else {
                    super.trustList(sender, profile.uuid());
                }
            } else {
                SchedulerUtil.schedule(plugin, sender, () -> BoltComponents.sendMessage(
                    sender,
                    Translation.PLAYER_NOT_FOUND,
                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(target))
                ));
            }
        });

    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        final String subcommand = arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("add", "remove", "list");
        }
        if ("list".equalsIgnoreCase(subcommand)) {
            return Collections.emptyList();
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
                Translation.HELP_COMMAND_SHORT_ADMIN_TRUST,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin trust")),
                Placeholder.component(Translation.Placeholder.LITERAL, Component.text("(add|remove|list)"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_TRUST);
    }
}

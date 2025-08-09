package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceTransformer;
import org.popcraft.bolt.source.SourceType;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModifyCommand extends BoltCommand {
    public ModifyCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        if (arguments.remaining() < 3) {
            shortHelp(sender, arguments);
            return;
        }
        final BoltPlayer boltPlayer = plugin.player(player);
        final boolean adding = "add".equalsIgnoreCase(arguments.next());
        final String accessType = arguments.next().toLowerCase();
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
        final List<String> identifiers = new ArrayList<>();
        if (sourceType.unique()) {
            identifiers.add(sourceType.name());
        } else {
            if (arguments.remaining() < 1) {
                shortHelp(sender, arguments);
                return;
            }
            String identifier;
            while ((identifier = arguments.next()) != null) {
                identifiers.add(identifier);
            }
        }
        boltPlayer.setAction(new Action(Action.Type.EDIT, "bolt.command.edit", Boolean.toString(adding)));
        for (final String identifier : identifiers) {
            final SourceTransformer sourceTransformer = plugin.getSourceTransformer(sourceType.name());
            sourceTransformer.transformIdentifier(identifier)
                    .thenAccept(id -> SchedulerUtil.schedule(plugin, player, () -> {
                        if (id == null) {
                            sourceTransformer.sendErrorNotFound(identifier, player);
                        } else {
                            boltPlayer.getModifications().put(Source.of(sourceType.name(), id), access.type());
                        }
                    }));
        }
        BoltComponents.sendMessage(
                player,
                Translation.CLICK_ACTION,
                plugin.isUseActionBar(),
                Placeholder.component(Translation.Placeholder.ACTION, BoltComponents.resolveTranslation(Translation.EDIT, player))
        );
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("add", "remove");
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getBolt().getAccessRegistry().access().stream()
                    .filter(access -> !access.restricted() || sender.hasPermission("bolt.type.access.%s".formatted(access.type())))
                    .map(Access::type)
                    .toList();
        }
        final String sourceType = arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getBolt().getSourceTypeRegistry().sourceTypes().stream()
                    .filter(type -> !type.restricted() || sender.hasPermission("bolt.type.source.%s".formatted(type.name())))
                    .map(SourceType::name)
                    .toList();
        }
        final Set<String> alreadyAdded = new HashSet<>();
        String added;
        while ((added = arguments.next()) != null) {
            alreadyAdded.add(added);
        }
        return plugin.getSourceTransformer(sourceType).completions(sender).stream().filter(name -> !alreadyAdded.contains(name)).toList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_MODIFY,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt modify")),
                Placeholder.component(Translation.Placeholder.LITERAL, Component.text("(add|remove)"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_MODIFY);
    }
}

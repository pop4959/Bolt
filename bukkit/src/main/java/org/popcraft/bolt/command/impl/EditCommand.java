package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.Collections;
import java.util.List;

public class EditCommand extends BoltCommand {
    public EditCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        if (arguments.remaining() < 2) {
            shortHelp(sender, arguments);
            return;
        }
        final BoltPlayer boltPlayer = plugin.player(player);
        final boolean adding = "add".equalsIgnoreCase(arguments.next());
        final String target = arguments.next();
        Profiles.findOrLookupProfileByName(target).thenAccept(playerProfile -> SchedulerUtil.schedule(plugin, sender, () -> {
            if (!playerProfile.complete()) {
                BoltComponents.sendMessage(
                        sender,
                        Translation.PLAYER_NOT_FOUND,
                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(target))
                );
                return;
            }
            final Source source = Source.player(playerProfile.uuid());
            boltPlayer.setAction(new Action(Action.Type.EDIT, "bolt.command.edit", Boolean.toString(adding)));
            boltPlayer.getModifications().put(source, plugin.getDefaultAccessType());
            BoltComponents.sendMessage(
                    player,
                    Translation.CLICK_ACTION,
                    plugin.isUseActionBar(),
                    Placeholder.component(Translation.Placeholder.ACTION, BoltComponents.resolveTranslation(Translation.EDIT, player))
            );
        }));
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
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_EDIT,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt edit")),
                Placeholder.component(Translation.Placeholder.LITERAL, Component.text("(add|remove)"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_EDIT);
    }
}

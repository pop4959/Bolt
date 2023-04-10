package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.Collections;
import java.util.List;

public class TransferCommand extends BoltCommand {
    public TransferCommand(BoltPlugin plugin) {
        super(plugin);
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
        final String owner = arguments.next();
        Profiles.findOrLookupProfileByName(owner).thenAccept(profile -> {
            if (profile.uuid() != null) {
                plugin.player(player).setAction(new Action(Action.Type.TRANSFER, profile.uuid().toString()));
                SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                        player,
                        Translation.CLICK_TRANSFER,
                        plugin.isUseActionBar()
                ));
            } else {
                SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                        player,
                        Translation.PLAYER_NOT_FOUND,
                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(owner))
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
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_TRANSFER,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt transfer"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_TRANSFER);
    }
}

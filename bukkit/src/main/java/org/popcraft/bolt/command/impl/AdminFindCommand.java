package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Pagination;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AdminFindCommand extends BoltCommand {

    public AdminFindCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() < 1) {
            shortHelp(sender, arguments);
            return;
        }
        final String player = arguments.next();
        Profiles.findOrLookupProfileByName(player).thenAccept(playerProfile -> SchedulerUtil.schedule(plugin, sender, () -> {
            if (!playerProfile.complete()) {
                BoltComponents.sendMessage(
                        sender,
                        Translation.PLAYER_NOT_FOUND,
                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(player))
                );
                return;
            }
            final List<Protection> protectionsFromPlayer = plugin.loadProtections().stream()
                    .filter(protection -> playerProfile.uuid().equals(protection.getOwner()))
                    .sorted(Comparator.comparingLong(Protection::getCreated).reversed())
                    .toList();
            Pagination.runPage(plugin, sender, protectionsFromPlayer, 0);
        }));
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
                Translation.HELP_COMMAND_SHORT_ADMIN_FIND,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin find"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_FIND);
    }
}

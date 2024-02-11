package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.Collections;
import java.util.List;

public class AdminPurgeCommand extends BoltCommand {
    public AdminPurgeCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() < 1) {
            shortHelp(sender, arguments);
            return;
        }
        final String owner = arguments.next();
        Profiles.findOrLookupProfileByName(owner).thenAccept(profile -> {
            if (profile.uuid() != null) {
                plugin.loadProtections().stream()
                        .filter(protection -> protection.getOwner().equals(profile.uuid()))
                        .forEach(plugin::removeProtection);
                SchedulerUtil.schedule(plugin, sender, () -> BoltComponents.sendMessage(
                        sender,
                        Translation.PURGE,
                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(owner))
                ));
            } else {
                SchedulerUtil.schedule(plugin, sender, () -> BoltComponents.sendMessage(
                        sender,
                        Translation.PLAYER_NOT_FOUND,
                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(owner))
                ));
            }
        });
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_PURGE,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin purge"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_PURGE);
    }
}

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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CallbackCommand extends BoltCommand {
    public CallbackCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final String argument = arguments.next();
        if (argument == null) {
            return;
        }
        final UUID uuid;
        try {
            uuid = UUID.fromString(argument);
        } catch (IllegalArgumentException e) {
            return;
        }

        plugin.getCallbackManager().execute(sender, uuid);
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        // idk
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        // idk
    }
}

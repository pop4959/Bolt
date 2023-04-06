package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelpCommand extends BoltCommand {
    public HelpCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final String command = arguments.next();
        if (command == null) {
            shortHelp(sender, arguments);
            longHelp(sender, arguments);
            return;
        }
        final BoltCommand boltCommand = plugin.commands().get(command);
        if (boltCommand == null) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_INVALID);
            return;
        }
        final Arguments shortCopy = arguments.copy();
        final Arguments longCopy = arguments.copy();
        final String subCommand = arguments.next();
        final BoltCommand finalCommand = "admin".equals(command) && subCommand != null ? AdminCommand.SUB_COMMANDS.get(subCommand) : boltCommand;
        finalCommand.shortHelp(sender, shortCopy);
        finalCommand.longHelp(sender, longCopy);
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        final String command = arguments.next();
        if (arguments.remaining() == 0) {
            return new ArrayList<>(plugin.commands().keySet());
        }
        arguments.next();
        if (arguments.remaining() == 0 && "admin".equals(command)) {
            return new ArrayList<>(AdminCommand.SUB_COMMANDS.keySet());
        }
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_HELP,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt help"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_HELP);
    }
}

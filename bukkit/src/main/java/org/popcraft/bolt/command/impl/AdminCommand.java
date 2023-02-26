package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCommand extends BoltCommand {
    private final Map<String, BoltCommand> subcommands = new HashMap<>();

    public AdminCommand(BoltPlugin plugin) {
        super(plugin);
        subcommands.putAll(Map.of(
                "convert", new AdminConvertCommand(plugin),
                "debug", new AdminDebugCommand(plugin),
                "report", new AdminReportCommand(plugin)
        ));
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final String subcommand = arguments.next();
        if (!subcommands.containsKey(subcommand)) {
            return;
        }
        subcommands.get(subcommand).execute(sender, arguments);
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        final String subcommand = arguments.next();
        if (!subcommands.containsKey(subcommand)) {
            return new ArrayList<>(subcommands.keySet());
        }
        return subcommands.get(subcommand).suggestions(arguments);
    }
}

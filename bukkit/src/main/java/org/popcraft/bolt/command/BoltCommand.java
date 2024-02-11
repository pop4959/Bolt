package org.popcraft.bolt.command;

import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;

import java.util.List;

public abstract class BoltCommand {
    protected BoltPlugin plugin;

    protected BoltCommand(BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract void execute(CommandSender sender, Arguments arguments);

    public abstract List<String> suggestions(CommandSender sender, Arguments arguments);

    public abstract void shortHelp(CommandSender sender, Arguments arguments);

    public abstract void longHelp(CommandSender sender, Arguments arguments);

    public boolean hidden() {
        return false;
    }
}

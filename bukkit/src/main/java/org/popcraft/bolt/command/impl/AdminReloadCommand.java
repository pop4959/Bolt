package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Collections;
import java.util.List;

public class AdminReloadCommand extends BoltCommand {
    public AdminReloadCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        plugin.reload();
        BoltComponents.sendMessage(sender, Translation.RELOAD);
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }
}

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
import org.popcraft.bolt.util.HangingCache;

import java.util.Collections;
import java.util.List;

public class AdminHDebugCommand extends BoltCommand {
    public AdminHDebugCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        plugin.getHangingCache().debug();
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {

    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
    }
}

package org.popcraft.bolt.command.implementation;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Collections;
import java.util.List;

public class DebugCommand extends BoltCommand {
    public DebugCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            plugin.getBolt().getBoltPlayer(player.getUniqueId()).addAction(Action.DEBUG);
            BoltComponents.sendMessage(player, "Click to debug object");
        }
    }

    @Override
    public List<String> suggestions() {
        return Collections.emptyList();
    }
}

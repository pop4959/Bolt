package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.lang.Translation;

import java.util.Collections;
import java.util.List;

public class PersistCommand extends BoltCommand {
    public PersistCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            final BoltPlayer boltPlayer = plugin.player(player);
            boltPlayer.togglePersist();
            BoltComponents.sendMessage(player, boltPlayer.isPersist() ? Translation.ACTION_PERSIST_ON : Translation.ACTION_PERSIST_OFF);
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        return Collections.emptyList();
    }
}

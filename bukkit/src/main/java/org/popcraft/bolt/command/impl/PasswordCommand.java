package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Collections;
import java.util.List;

public class PasswordCommand extends BoltCommand {
    public PasswordCommand(BoltPlugin plugin) {
        super(plugin);
    }

    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            final BoltPlayer boltPlayer = plugin.player(player);
            if (arguments.remaining() > 0) {
                boltPlayer.addPassword(arguments.next());
                BoltComponents.sendMessage(player, Translation.ENTER_PASSWORD);
            } else {
                BoltComponents.sendMessage(player, Translation.ENTER_PASSWORD_NONE);
            }
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        return Collections.emptyList();
    }
}

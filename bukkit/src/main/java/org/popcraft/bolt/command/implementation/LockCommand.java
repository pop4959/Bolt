package org.popcraft.bolt.command.implementation;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.Bolt;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BukkitCommand;
import org.popcraft.bolt.util.Action;

public class LockCommand extends BukkitCommand {
    public LockCommand(Bolt bolt) {
        super(bolt);
    }

    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            bolt.getBoltPlayer(player.getUniqueId()).addAction(Action.LOCK_BLOCK);

            player.sendMessage("Click on a block to lock");
        }
    }
}

package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Source;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Collections;
import java.util.List;

public class ModifyCommand extends BoltCommand {
    public ModifyCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player && arguments.remaining() >= 3) {
            final PlayerMeta playerMeta = plugin.getBolt().getPlayerMeta(player.getUniqueId());
            playerMeta.addAction(Action.MODIFY);
            final String sourceType = arguments.next();
            final String sourceIdentifier = arguments.next();
            final String access = arguments.next();
            playerMeta.getModifications().put(new Source(sourceType, sourceIdentifier), access);
            BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_MODIFY);
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions() {
        return Collections.emptyList();
    }
}

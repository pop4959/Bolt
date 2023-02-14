package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.lang.Translation;

import java.util.Collections;
import java.util.List;

import static org.popcraft.bolt.lang.Translator.translate;

public class LockCommand extends BoltCommand {
    public LockCommand(BoltPlugin plugin) {
        super(plugin);
    }

    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            final BoltPlayer boltPlayer = plugin.player(player);
            boltPlayer.setAction(Action.LOCK);
            if (arguments.remaining() > 0) {
                boltPlayer.setLockNil(true);
            }
            BoltComponents.sendMessage(player, Translation.CLICK_ACTION, Placeholder.unparsed("action", translate(Translation.LOCK)));
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        return Collections.emptyList();
    }
}

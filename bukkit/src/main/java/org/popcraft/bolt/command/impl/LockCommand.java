package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Collections;
import java.util.List;

import static org.popcraft.bolt.util.lang.Translator.translate;

public class LockCommand extends BoltCommand {
    public LockCommand(BoltPlugin plugin) {
        super(plugin);
    }

    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            plugin.playerMeta(player).addAction(Action.LOCK);
            BoltComponents.sendMessage(player, Translation.CLICK_ACTION, Template.of("action", translate(Translation.LOCK)));
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions() {
        return Collections.emptyList();
    }
}

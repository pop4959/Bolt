package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Collections;
import java.util.List;

import static org.popcraft.bolt.util.lang.Translator.translate;

public class PersistCommand extends BoltCommand {
    public PersistCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            final PlayerMeta playerMeta = plugin.playerMeta(player);
            playerMeta.togglePersist();
            BoltComponents.sendMessage(player, Translation.COMMAND_PERSIST, Template.of("toggle", translate(playerMeta.isPersist() ? Translation.ENABLED : Translation.DISABLED)));
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions() {
        return Collections.emptyList();
    }
}

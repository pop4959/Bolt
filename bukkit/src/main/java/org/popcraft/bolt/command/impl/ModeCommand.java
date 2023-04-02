package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Mode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.popcraft.bolt.lang.Translator.translate;

public class ModeCommand extends BoltCommand {
    public ModeCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (sender instanceof final Player player) {
            final BoltPlayer boltPlayer = plugin.player(player);
            final String modeArgument = arguments.next();
            if (modeArgument == null) {
                BoltComponents.sendMessage(player, Translation.MODE_INVALID);
                return;
            }
            final Mode mode;
            try {
                mode = Mode.valueOf(modeArgument.toUpperCase());
            } catch (IllegalArgumentException e) {
                BoltComponents.sendMessage(player, Translation.MODE_INVALID);
                return;
            }
            boltPlayer.toggleMode(mode);
            BoltComponents.sendMessage(
                    player,
                    boltPlayer.hasMode(mode) ? Translation.MODE_ENABLED : Translation.MODE_DISABLED,
                    Placeholder.unparsed(Translation.Placeholder.MODE, translate("mode_%s".formatted(mode.name().toLowerCase())))
            );
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return Arrays.stream(Mode.values()).map(mode -> mode.name().toLowerCase()).toList();
        }
        return Collections.emptyList();
    }
}

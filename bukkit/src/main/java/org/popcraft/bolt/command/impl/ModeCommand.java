package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Mode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
            final boolean hasMode = boltPlayer.hasMode(mode);
            BoltComponents.sendMessage(
                    player,
                    hasMode ? Translation.MODE_ENABLED : Translation.MODE_DISABLED,
                    Placeholder.unparsed(Translation.Placeholder.MODE, translate("mode_%s".formatted(mode.name().toLowerCase())))
            );
            final UUID uuid = player.getUniqueId();
            CompletableFuture.runAsync(() -> {
                final File playerFile = plugin.getDataPath().resolve("players/%s.yml".formatted(uuid)).toFile();
                final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                playerConfig.set(mode.name().toLowerCase(), hasMode);
                try {
                    playerConfig.save(playerFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
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

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_MODE,
                Placeholder.unparsed(Translation.Placeholder.COMMAND, "/bolt mode")
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_MODE);
    }
}

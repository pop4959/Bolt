package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;

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
            final String argument = arguments.next();
            final String type = argument == null ? plugin.getDefaultProtectionType() : argument;
            final Access access = plugin.getBolt().getAccessRegistry().getProtectionByType(type).orElse(null);
            if (access == null) {
                BoltComponents.sendMessage(
                        sender,
                        Translation.CLICK_LOCKED_NO_EXIST,
                        Placeholder.unparsed(Translation.Placeholder.PROTECTION_TYPE, type)
                );
                return;
            }
            if (access.restricted() && !sender.hasPermission("bolt.type.protection.%s".formatted(access.type()))) {
                BoltComponents.sendMessage(sender, Translation.CLICK_LOCKED_NO_PERMISSION);
                return;
            }
            boltPlayer.setAction(new Action(Action.Type.LOCK, type));
            if (BoltPlugin.DEBUG && arguments.remaining() > 0) {
                boltPlayer.setLockNil(true);
            }
            BoltComponents.sendMessage(
                    player,
                    Translation.CLICK_ACTION,
                    plugin.isUseActionBar(),
                    Placeholder.unparsed(Translation.Placeholder.ACTION, translate(Translation.LOCK))
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
            return plugin.getBolt().getAccessRegistry().protections()
                    .stream().filter(protection -> !protection.restricted() || sender.hasPermission("bolt.type.protection.%s".formatted(protection.type())))
                    .map(Access::type)
                    .toList();
        }
        return Collections.emptyList();
    }
}

package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.Access;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.lang.Translation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.popcraft.bolt.lang.Translator.translate;

public class EditCommand extends BoltCommand {
    public EditCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        if (arguments.remaining() < 3) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_NOT_ENOUGH_ARGS);
            return;
        }
        final BoltPlayer boltPlayer = plugin.player(player);
        final boolean adding = "add".equalsIgnoreCase(arguments.next());
        boltPlayer.setAction(new Action(Action.Type.EDIT, Boolean.toString(adding)));
        final Access access = plugin.getBolt().getAccessRegistry().getAccessByType(arguments.next()).orElse(null);
        if (access == null) {
            BoltComponents.sendMessage(sender, Translation.EDIT_ACCESS_INVALID);
            return;
        }
        String source;
        while ((source = arguments.next()) != null) {
            if (Source.REDSTONE.equals(source) || Source.BLOCK.equals(source)) {
                boltPlayer.getModifications().put(Source.from(source), access.type());
            } else if (source.startsWith(Source.PASSWORD)) {
                final String[] split = source.split(":");
                if (split.length < 2) {
                    BoltComponents.sendMessage(sender, Translation.EDIT_PASSWORD_INVALID);
                    return;
                }
                boltPlayer.getModifications().put(Source.fromPassword(split[1]), access.type());
            } else {
                final UUID uuid = BukkitAdapter.findPlayerUniqueId(source);
                if (uuid != null) {
                    boltPlayer.getModifications().put(Source.fromPlayer(uuid), access.type());
                } else {
                    BoltComponents.sendMessage(player, Translation.PLAYER_NOT_FOUND, Placeholder.unparsed("player", source));
                }
            }
        }
        BoltComponents.sendMessage(player, Translation.CLICK_ACTION, Placeholder.unparsed("action", translate(Translation.EDIT)));
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("add", "remove");
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getBolt().getAccessRegistry().accessTypes();
        }
        final Set<String> alreadyAdded = new HashSet<>();
        String added;
        while ((added = arguments.next()) != null) {
            alreadyAdded.add(added);
        }
        return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> !alreadyAdded.contains(name)).toList();
    }
}

package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceType;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.BukkitAdapter;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        final String accessType = arguments.next();
        final Access access = plugin.getBolt().getAccessRegistry().getAccessByType(accessType).orElse(null);
        if (access == null) {
            BoltComponents.sendMessage(sender, Translation.EDIT_ACCESS_INVALID, Placeholder.unparsed("access", accessType));
            return;
        }
        final String sourceType = arguments.next();
        if (sourceType == null || !plugin.getBolt().getSourceTypeRegistry().sourceTypes().contains(sourceType)) {
            BoltComponents.sendMessage(sender, Translation.EDIT_SOURCE_INVALID, Placeholder.unparsed("source", sourceType));
            return;
        }
        String identifier;
        while ((identifier = arguments.next()) != null) {
            if (SourceType.PLAYER.equals(sourceType)) {
                String finalIdentifier = identifier;
                BukkitAdapter.findOrLookupProfileByName(identifier).thenAccept(profile -> {
                    if (profile.uuid() != null) {
                        boltPlayer.getModifications().put(Source.player(profile.uuid()), access.type());
                    } else {
                        BoltComponents.sendMessage(player, Translation.PLAYER_NOT_FOUND, Placeholder.unparsed("player", finalIdentifier));
                    }
                });
            } else if (SourceType.PASSWORD.equals(sourceType)) {
                boltPlayer.getModifications().put(Source.password(identifier), access.type());
            } else {
                boltPlayer.getModifications().put(Source.of(sourceType, identifier), access.type());
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
        final String sourceType = arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getBolt().getSourceTypeRegistry().sourceTypes().stream().toList();
        }
        if (!SourceType.PLAYER.equals(sourceType)) {
            return Collections.emptyList();
        }
        final Set<String> alreadyAdded = new HashSet<>();
        String added;
        while ((added = arguments.next()) != null) {
            alreadyAdded.add(added);
        }
        return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> !alreadyAdded.contains(name)).toList();
    }
}

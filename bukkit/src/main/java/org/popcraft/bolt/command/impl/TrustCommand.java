package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.AccessList;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Protections;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TrustCommand extends BoltCommand {
    public TrustCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        final String action = arguments.next();
        if ("list".equals(action)) {
            final AccessList accessList = plugin.getBolt().getStore().loadAccessList(player.getUniqueId()).join();
            final Map<String, String> accessMap = accessList == null ? new HashMap<>() : accessList.getAccess();
            BukkitAdapter.findOrLookupProfileByUniqueId(player.getUniqueId()).thenAccept(profile -> BoltComponents.sendMessage(player, Translation.INFO_SELF, Placeholder.unparsed(Translation.Placeholder.ACCESS_LIST_SIZE, String.valueOf(accessMap.size())), Placeholder.unparsed(Translation.Placeholder.ACCESS_LIST, Protections.accessList(accessMap))));
        } else if (!"confirm".equals(action)) {
            BoltComponents.sendMessage(sender, Translation.TRUST_CONFIRM, Placeholder.unparsed(Translation.Placeholder.COMMAND, "/bolt trust confirm"));
        } else {
            final BoltPlayer boltPlayer = plugin.player(player);
            final Action editAction = Optional.ofNullable(boltPlayer.getAction())
                    .filter(a -> Action.Type.EDIT.equals(a.getType()))
                    .orElse(null);
            if (editAction == null) {
                BoltComponents.sendMessage(sender, Translation.TRUST_EDITED_FAILED, Placeholder.unparsed(Translation.Placeholder.COMMAND, "/bolt edit"));
                return;
            }
            final UUID uuid = player.getUniqueId();
            final AccessList accessList = Objects.requireNonNullElse(plugin.getBolt().getStore().loadAccessList(uuid).join(), new AccessList(uuid, new HashMap<>()));
            boltPlayer.consumeModifications().forEach((source, type) -> {
                if (Boolean.parseBoolean(editAction.getData())) {
                    accessList.getAccess().put(source.toString(), type);
                } else {
                    accessList.getAccess().remove(source.toString());
                }
            });
            plugin.getBolt().getStore().saveAccessList(accessList);
            BoltComponents.sendMessage(sender, Translation.TRUST_EDITED);
            boltPlayer.clearAction();
        }
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("list");
        }
        return Collections.emptyList();
    }
}

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
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Protections;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoCommand extends BoltCommand {
    public InfoCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final String defaults = arguments.next();
        if (sender instanceof final Player player) {
            if ("default".equals(defaults)) {
                final AccessList accessList = plugin.getBolt().getStore().loadAccessList(player.getUniqueId()).join();
                final Map<String, String> accessMap = accessList == null ? new HashMap<>() : accessList.getAccess();
                BukkitAdapter.findOrLookupProfileByUniqueId(player.getUniqueId()).thenAccept(profile -> BoltComponents.sendMessage(player, Translation.INFO_SELF, Placeholder.unparsed("access_count", String.valueOf(accessMap.size())), Placeholder.unparsed("access_list", Protections.accessList(accessMap))));
            } else {
                plugin.player(player).setAction(new Action(Action.Type.INFO));
                BoltComponents.sendMessage(player, Translation.CLICK_INFO);
            }
        } else {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
        }
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("default");
        }
        return Collections.emptyList();
    }
}

package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;

import java.util.Collections;
import java.util.List;

public class AdminPurgeCommand extends BoltCommand {
    public AdminPurgeCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        if (arguments.remaining() < 1) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_NOT_ENOUGH_ARGS);
            return;
        }
        final String owner = arguments.next();
        BukkitAdapter.findOrLookupPlayerUniqueId(owner).thenAccept(uuid -> {
            if (uuid != null) {
                final Store store = plugin.getBolt().getStore();
                store.loadBlockProtections().join().stream()
                        .filter(protection -> protection.getOwner().equals(uuid))
                        .forEach(store::removeBlockProtection);
                store.loadEntityProtections().join().stream()
                        .filter(protection -> protection.getOwner().equals(uuid))
                        .forEach(store::removeEntityProtection);
                BoltComponents.sendMessage(player, Translation.PURGE, Placeholder.unparsed("player", owner));
            } else {
                BoltComponents.sendMessage(player, Translation.PLAYER_NOT_FOUND, Placeholder.unparsed("player", owner));
            }
        });
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        return Collections.emptyList();
    }
}

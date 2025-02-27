package org.popcraft.bolt.source;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;

import java.util.List;

public class GroupSourceTransformer implements SourceTransformer {
    private final BoltPlugin plugin;

    public GroupSourceTransformer(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> completions(CommandSender sender) {
        if (sender instanceof final Player player) {
            return plugin.getPlayersOwnedGroups(player);
        }
        return List.of();
    }
}

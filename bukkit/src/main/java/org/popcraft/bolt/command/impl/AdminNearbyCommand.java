package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Pagination;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AdminNearbyCommand extends BoltCommand {

    public AdminNearbyCommand(BoltPlugin plugin) {
        super(plugin);
    }

    private Location protectionLocation(Protection protection) {
        return switch (protection) {
            case BlockProtection block ->
                    new Location(plugin.getServer().getWorld(block.getWorld()), block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
            case EntityProtection entityProtection -> {
                final Entity entity = plugin.getServer().getEntity(entityProtection.getId());
                yield entity == null ? null : entity.getLocation();
            }
        };
    }

    private double distance(Location a, Protection protection) {
        final Location b = protectionLocation(protection);
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return Double.NaN;
        }
        return a.distance(b);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        final Integer limit = arguments.nextAsInteger();
        if (limit == null) {
            shortHelp(sender, arguments);
            return;
        }
        final Location playerLocation = player.getLocation();
        // TODO: could be more efficient with distance calculations
        final List<Protection> closestProtections = plugin.loadProtections().stream()
                .filter(protection -> {
                    final double d = distance(playerLocation, protection);
                    return !Double.isNaN(d) && d < limit;
                })
                .sorted(Comparator.comparingDouble(protection -> distance(playerLocation, protection)))
                .toList();
        Pagination.runPage(plugin, sender, closestProtections, 0);
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_NEARBY,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin nearby"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_NEARBY);
    }
}

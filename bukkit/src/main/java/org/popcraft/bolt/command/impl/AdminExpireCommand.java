package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AdminExpireCommand extends BoltCommand {
    public AdminExpireCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final Store store = plugin.getBolt().getStore();
        final Collection<BlockProtection> protections = store.loadBlockProtections().join();
        final long now = System.currentTimeMillis();
        final Integer timeValue = arguments.nextAsInteger();
        final TimeUnit timeUnit = Optional.ofNullable(arguments.next())
                .map(arg -> {
                    try {
                        return TimeUnit.valueOf(arg.toUpperCase());
                    } catch (final IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);
        if (timeValue == null || timeUnit == null) {
            BoltComponents.sendMessage(sender, Translation.EXPIRE_INVALID_TIME);
            return;
        }
        final long timeDiffMillis = TimeUnit.MILLISECONDS.convert(timeValue, timeUnit);
        final long expireTime = now - timeDiffMillis;
        long removed = 0;
        for (final BlockProtection blockProtection : protections) {
            final long lastAccessed = blockProtection.getAccessed();
            if (lastAccessed > 0 && lastAccessed < expireTime) {
                store.removeBlockProtection(blockProtection);
                ++removed;
            }
        }
        BoltComponents.sendMessage(
                sender,
                Translation.EXPIRE_COMPLETE,
                Placeholder.component(Translation.Placeholder.COUNT, Component.text(removed))
        );
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_EXPIRE,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin expire"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_EXPIRE);
    }
}

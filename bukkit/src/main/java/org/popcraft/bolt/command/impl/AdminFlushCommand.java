package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Collections;
import java.util.List;

public class AdminFlushCommand extends BoltCommand {
    public AdminFlushCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final Store store = plugin.getBolt().getStore();
        BoltComponents.sendMessage(
                sender,
                Translation.FLUSH,
                Placeholder.component(Translation.Placeholder.COUNT, Component.text(store.pendingSave()))
        );
        store.flush().join();
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_FLUSH,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin flush"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_FLUSH);
    }
}

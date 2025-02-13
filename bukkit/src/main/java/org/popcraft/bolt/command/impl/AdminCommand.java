package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BoltComponents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCommand extends BoltCommand {
    public static final Map<String, BoltCommand> SUB_COMMANDS = new HashMap<>();
    private static final String COMMAND_PERMISSION_KEY = "bolt.command.admin.";

    public AdminCommand(BoltPlugin plugin) {
        super(plugin);
        SUB_COMMANDS.clear();
        SUB_COMMANDS.putAll(Map.ofEntries(
                Map.entry("cleanup", new AdminCleanup(plugin)),
                Map.entry("convert", new AdminConvertCommand(plugin)),
                Map.entry("debug", new AdminDebugCommand(plugin)),
                Map.entry("expire", new AdminExpireCommand(plugin)),
                Map.entry("find", new AdminFindCommand(plugin)),
                Map.entry("flush", new AdminFlushCommand(plugin)),
                Map.entry("purge", new AdminPurgeCommand(plugin)),
                Map.entry("reload", new AdminReloadCommand(plugin)),
                Map.entry("report", new AdminReportCommand(plugin)),
                Map.entry("transfer", new AdminTransferCommand(plugin)),
                Map.entry("trust", new AdminTrustCommand(plugin))
        ));
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final String subcommand = arguments.next();
        if (subcommand == null) {
            final Collection<Protection> protections = plugin.loadProtections();
            final long blockCount = protections.stream().filter(BlockProtection.class::isInstance).count();
            final long entityCount = protections.stream().filter(EntityProtection.class::isInstance).count();
            BoltComponents.sendMessage(
                    sender,
                    Translation.STATUS,
                    Placeholder.component(Translation.Placeholder.COUNT_BLOCKS, Component.text(blockCount)),
                    Placeholder.component(Translation.Placeholder.COUNT_ENTITIES, Component.text(entityCount))
            );
            return;
        }
        if (!SUB_COMMANDS.containsKey(subcommand)) {
            return;
        }
        if (!sender.hasPermission(COMMAND_PERMISSION_KEY + subcommand)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_NO_PERMISSION);
            return;
        }
        SUB_COMMANDS.get(subcommand).execute(sender, arguments);
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        final String subcommand = arguments.next();
        if (!SUB_COMMANDS.containsKey(subcommand)) {
            return new ArrayList<>(SUB_COMMANDS.keySet());
        }
        return SUB_COMMANDS.get(subcommand).suggestions(sender, arguments);
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        final String subcommand = arguments.next();
        if (!SUB_COMMANDS.containsKey(subcommand)) {
            BoltComponents.sendMessage(
                    sender,
                    Translation.HELP_COMMAND_SHORT_ADMIN,
                    Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin"))
            );
            return;
        }
        SUB_COMMANDS.get(subcommand).shortHelp(sender, arguments);
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        final String subcommand = arguments.next();
        if (!SUB_COMMANDS.containsKey(subcommand)) {
            BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN);
            return;
        }
        SUB_COMMANDS.get(subcommand).longHelp(sender, arguments);
    }
}

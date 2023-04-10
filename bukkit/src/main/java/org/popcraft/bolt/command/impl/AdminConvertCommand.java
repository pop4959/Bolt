package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.data.migration.lwc.BoltMigration;
import org.popcraft.bolt.data.migration.lwc.LWCMigration;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminConvertCommand extends BoltCommand {
    private final AtomicBoolean isConverting = new AtomicBoolean();
    private LWCMigration lastMigration;

    public AdminConvertCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (isConverting.get()) {
            BoltComponents.sendMessage(sender, Translation.MIGRATION_IN_PROGRESS);
            return;
        }
        final String arg = arguments.next();
        final boolean convertEntities = "entities".equalsIgnoreCase(arg);
        if (lastMigration != null && convertEntities) {
            BoltComponents.sendMessage(
                    sender,
                    Translation.MIGRATION_STARTED,
                    Placeholder.component(Translation.Placeholder.OLD_PLUGIN, Component.text("LWC")),
                    Placeholder.component(Translation.Placeholder.NEW_PLUGIN, Component.text("Bolt"))
            );
            isConverting.set(true);
            lastMigration.convertEntityBlocks().whenCompleteAsync((memoryStore, throwable) -> {
                isConverting.set(false);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                for (final EntityProtection entityProtection : memoryStore.loadEntityProtections().join()) {
                    plugin.saveProtection(entityProtection);
                }
                BoltComponents.sendMessage(sender, Translation.MIGRATION_COMPLETED);
            }, SchedulerUtil.executor(plugin, sender));
            lastMigration = null;
            return;
        }
        final boolean convertBack = "back".equalsIgnoreCase(arg);
        if (convertBack) {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("LWC")) {
                BoltComponents.sendMessage(sender, Translation.MIGRATION_LWC_MISSING);
                return;
            }
            final BoltMigration migration = new BoltMigration(plugin);
            BoltComponents.sendMessage(
                    sender,
                    Translation.MIGRATION_STARTED,
                    Placeholder.component(Translation.Placeholder.OLD_PLUGIN, Component.text("Bolt")),
                    Placeholder.component(Translation.Placeholder.NEW_PLUGIN, Component.text("LWC"))
            );
            isConverting.set(true);
            migration.convertAsync().whenCompleteAsync((ignored, throwable) -> {
                isConverting.set(false);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                BoltComponents.sendMessage(sender, Translation.MIGRATION_COMPLETED);
            }, SchedulerUtil.executor(plugin, sender));
        } else {
            final LWCMigration migration = new LWCMigration(plugin);
            BoltComponents.sendMessage(
                    sender,
                    Translation.MIGRATION_STARTED,
                    Placeholder.component(Translation.Placeholder.OLD_PLUGIN, Component.text("LWC")),
                    Placeholder.component(Translation.Placeholder.NEW_PLUGIN, Component.text("Bolt"))
            );
            isConverting.set(true);
            migration.convertAsync().whenCompleteAsync((memoryStore, throwable) -> {
                isConverting.set(false);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                for (final BlockProtection blockProtection : memoryStore.loadBlockProtections().join()) {
                    plugin.saveProtection(blockProtection);
                }
                for (final EntityProtection entityProtection : memoryStore.loadEntityProtections().join()) {
                    plugin.saveProtection(entityProtection);
                }
                BoltComponents.sendMessage(sender, Translation.MIGRATION_COMPLETED);
                if (migration.hasEntityBlocks()) {
                    lastMigration = migration;
                    BoltComponents.sendMessage(
                            sender,
                            Translation.MIGRATION_COMPLETED_FOUND_ENTITIES,
                            Placeholder.component("command", Component.text("/bolt admin convert entities"))
                    );
                }
            }, SchedulerUtil.executor(plugin, sender));
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            final List<String> suggestions = new ArrayList<>(List.of("back"));
            if (lastMigration != null && lastMigration.hasEntityBlocks()) {
                suggestions.add("entities");
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_CONVERT,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin convert"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_CONVERT);
    }
}

package org.popcraft.bolt.command.impl;

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
import org.popcraft.bolt.util.BukkitMainThreadExecutor;

import java.util.Collections;
import java.util.List;

public class AdminConvertCommand extends BoltCommand {
    public AdminConvertCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final boolean convertBack = arguments.remaining() > 0 && arguments.next().equals("back");
        if (convertBack) {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("LWC")) {
                BoltComponents.sendMessage(sender, Translation.MIGRATION_LWC_MISSING);
                return;
            }
            final BoltMigration migration = new BoltMigration(plugin);
            BoltComponents.sendMessage(sender, Translation.MIGRATION_STARTED, Placeholder.unparsed("source", "Bolt"), Placeholder.unparsed("destination", "LWC"));
            migration.convertAsync().thenRunAsync(() -> BoltComponents.sendMessage(sender, Translation.MIGRATION_COMPLETED), BukkitMainThreadExecutor.get());
        } else {
            final LWCMigration migration = new LWCMigration(plugin);
            BoltComponents.sendMessage(sender, Translation.MIGRATION_STARTED, Placeholder.unparsed("source", "LWC"), Placeholder.unparsed("destination", "Bolt"));
            migration.convertAsync().thenAcceptAsync(memoryStore -> {
                final Store destination = plugin.getBolt().getStore();
                for (final BlockProtection blockProtection : memoryStore.loadBlockProtections().join()) {
                    destination.saveBlockProtection(blockProtection);
                }
                for (final EntityProtection entityProtection : memoryStore.loadEntityProtections().join()) {
                    destination.saveEntityProtection(entityProtection);
                }
                BoltComponents.sendMessage(sender, Translation.MIGRATION_COMPLETED);
            }, BukkitMainThreadExecutor.get());
        }
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("back");
        }
        return Collections.emptyList();
    }
}

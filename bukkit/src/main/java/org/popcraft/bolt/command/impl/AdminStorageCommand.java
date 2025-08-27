package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.SQLStore;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.SchedulerUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminStorageCommand extends BoltCommand {
    private final AtomicBoolean isConverting = new AtomicBoolean();

    public AdminStorageCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final String method = arguments.next();
        if (!"export".equalsIgnoreCase(method) && !"import".equalsIgnoreCase(method)) {
            shortHelp(sender, arguments);
            return;
        }
        if (isConverting.get()) {
            BoltComponents.sendMessage(sender, Translation.STORAGE_IN_PROGRESS);
            return;
        }

        final Path exportPath = plugin.getDataPath().resolve("export.db");
        final SQLStore.Configuration databaseConfiguration = new SQLStore.Configuration(
                "sqlite", exportPath.toString(), "", "", "", "", "", Map.of()
        );

        final Store currentStore = plugin.getBolt().getStore();

        if ("export".equalsIgnoreCase(method)) {
            if (Files.exists(exportPath)) {
                BoltComponents.sendMessage(sender, Translation.STORAGE_EXPORT_EXISTS);
                return;
            }
            final SQLStore exportStore = new SQLStore(databaseConfiguration);
            BoltComponents.sendMessage(sender, Translation.STORAGE_EXPORT_STARTED);
            this.isConverting.set(true);
            this.transfer(currentStore, exportStore).whenCompleteAsync((v, throwable) -> {
                this.isConverting.set(false);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                BoltComponents.sendMessage(sender, Translation.STORAGE_EXPORT_COMPLETED);
                exportStore.close();
            }, SchedulerUtil.executor(plugin, sender));
        } else {
            if (!Files.exists(exportPath)) {
                BoltComponents.sendMessage(sender, Translation.STORAGE_IMPORT_DOESNT_EXIST);
                return;
            }
            final SQLStore exportStore = new SQLStore(databaseConfiguration);
            BoltComponents.sendMessage(sender, Translation.STORAGE_IMPORT_STARTED);
            this.isConverting.set(true);
            this.transfer(exportStore, currentStore).whenCompleteAsync((v, throwable) -> {
                this.isConverting.set(false);
                if (throwable != null) {
                    throwable.printStackTrace();
                }
                BoltComponents.sendMessage(sender, Translation.STORAGE_IMPORT_COMPLETED);
                exportStore.close();
            }, SchedulerUtil.executor(plugin, sender));
        }
    }

    private CompletableFuture<Void> transfer(final Store from, final Store to) {
        return CompletableFuture.runAsync(() -> {
            from.loadBlockProtections().join().forEach(to::saveBlockProtection);
            from.loadEntityProtections().join().forEach(to::saveEntityProtection);
            from.loadGroups().join().forEach(to::saveGroup);
            from.loadAccessLists().join().forEach(to::saveAccessList);
            to.flush().join();
        });
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        return List.of("export", "import");
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_STORAGE,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin storage")),
                Placeholder.component(Translation.Placeholder.LITERAL, Component.text("(export|import)"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_STORAGE);
    }
}

package org.popcraft.bolt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.command.impl.DebugCommand;
import org.popcraft.bolt.command.impl.InfoCommand;
import org.popcraft.bolt.command.impl.LockCommand;
import org.popcraft.bolt.command.impl.ModifyCommand;
import org.popcraft.bolt.command.impl.UnlockCommand;
import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.store.SQLiteStore;
import org.popcraft.bolt.store.Store;
import org.popcraft.bolt.event.AccessEvents;
import org.popcraft.bolt.event.DebugEvents;
import org.popcraft.bolt.event.EnvironmentEvents;
import org.popcraft.bolt.event.RegistrationEvents;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.lang.Translation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoltPlugin extends JavaPlugin {
    private static final String COMMAND_PERMISSION_KEY = "bolt.command.";
    private final Bolt bolt = new Bolt(new SQLiteStore());
    private final Map<String, BoltCommand> commands = new HashMap<>();

    @Override
    public void onEnable() {
        BoltComponents.enable(this);
        registerEvents();
        registerCommands();
        listAllBlockProtections(bolt.getStore());
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new AccessEvents(this), this);
        getServer().getPluginManager().registerEvents(new DebugEvents(this), this);
        getServer().getPluginManager().registerEvents(new EnvironmentEvents(this), this);
        getServer().getPluginManager().registerEvents(new RegistrationEvents(this), this);
    }

    private void registerCommands() {
        commands.put("debug", new DebugCommand(this));
        commands.put("info", new InfoCommand(this));
        commands.put("lock", new LockCommand(this));
        commands.put("modify", new ModifyCommand(this));
        commands.put("unlock", new UnlockCommand(this));
    }

    @Override
    public void onDisable() {
        BoltComponents.disable();
        HandlerList.unregisterAll(this);
        commands.clear();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0 && commands.containsKey(args[0].toLowerCase())) {
            if (sender.hasPermission(COMMAND_PERMISSION_KEY + args[0].toLowerCase())) {
                commands.get(args[0].toLowerCase()).execute(sender, new Arguments(Arrays.copyOfRange(args, 1, args.length)));
            } else {
                BoltComponents.sendMessage(sender, Translation.COMMAND_NO_PERMISSION);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length < 1) {
            return Collections.emptyList();
        }
        final List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            commands.keySet().stream().filter(name -> sender.hasPermission(COMMAND_PERMISSION_KEY + name)).forEach(suggestions::add);
        } else if (commands.containsKey(args[0].toLowerCase()) && sender.hasPermission(COMMAND_PERMISSION_KEY + args[0].toLowerCase())) {
            suggestions.addAll(commands.get(args[0].toLowerCase()).suggestions());
        }
        return suggestions.stream()
                .filter(s -> s.toLowerCase().contains(args[args.length - 1].toLowerCase()))
                .toList();
    }

    public Bolt getBolt() {
        return bolt;
    }

    private void listAllBlockProtections(final Store store) {
        final List<BlockProtection> sqliteProtections = store.loadBlockProtections();
        sqliteProtections.forEach(protection -> getLogger().info(() -> new ProtectionData(
                UUID.randomUUID(),
                protection.getOwner(),
                protection.getType(),
                protection.getAccessList(),
                protection.getBlock(),
                protection.getWorld(),
                protection.getX(),
                protection.getY(),
                protection.getZ()
        ).toString()));
    }

    private record ProtectionData(UUID id, UUID owner, String type, Map<Source, String> accessList, String block,
                                  String world, int x, int y, int z) {
    }
}

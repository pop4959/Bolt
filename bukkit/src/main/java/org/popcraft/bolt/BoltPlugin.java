package org.popcraft.bolt;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.command.implementation.DebugCommand;
import org.popcraft.bolt.command.implementation.LockCommand;
import org.popcraft.bolt.command.implementation.UnlockCommand;
import org.popcraft.bolt.data.Source;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.store.SQLiteStore;
import org.popcraft.bolt.data.store.Store;
import org.popcraft.bolt.event.AccessEvents;
import org.popcraft.bolt.event.DebugEvents;
import org.popcraft.bolt.event.EnvironmentEvents;
import org.popcraft.bolt.event.RegistrationEvents;
import org.popcraft.bolt.util.BoltComponents;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoltPlugin extends JavaPlugin {
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
        commands.put("lock", new LockCommand(this));
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
            commands.get(args[0].toLowerCase()).execute(sender, new Arguments(Arrays.copyOfRange(args, 1, args.length)));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return Collections.emptyList();
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

    private record ProtectionData(UUID id, String owner, String type, Map<Source, String> accessList, String block,
                                  String world, int x, int y, int z) {
    }
}

package org.popcraft.bolt;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BukkitCommand;
import org.popcraft.bolt.command.implementation.LockCommand;
import org.popcraft.bolt.data.Source;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.store.SQLiteStore;
import org.popcraft.bolt.data.store.Store;
import org.popcraft.bolt.event.AccessEvents;
import org.popcraft.bolt.event.EnvironmentEvents;
import org.popcraft.bolt.event.RegistrationEvents;
import org.popcraft.bolt.migration.lwc.LWCMigration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.LogManager;

public class BoltPlugin extends JavaPlugin {
    private Bolt bolt;
    private BukkitAudiences adventure;
    private final Store store = new LWCMigration().migrate();
    private final Map<String, BukkitCommand> commands = new HashMap<>();

    @Override
    public void onEnable() {
        this.bolt = new Bolt();
        this.adventure = BukkitAudiences.create(this);
        commands.put("lock", new LockCommand(bolt));
        store.loadAccess().forEach(access -> bolt.getAccessRegistry().register(access.type(), access));
        getServer().getPluginManager().registerEvents(new AccessEvents(this), this);
        getServer().getPluginManager().registerEvents(new EnvironmentEvents(this), this);
        getServer().getPluginManager().registerEvents(new RegistrationEvents(this), this);
        final List<BlockProtection> protectedBlocks = store.loadBlockProtections();
        getLogger().info(() -> String.valueOf(protectedBlocks.size()));
        final SQLiteStore sqLiteStore = new SQLiteStore();
        getLogger().info("Starting conversion");
        final long millis = System.currentTimeMillis();
        protectedBlocks.forEach(sqLiteStore::saveBlockProtection);
        bolt.setStore(sqLiteStore);
        getLogger().info(() -> "Finished at %d".formatted(System.currentTimeMillis() - millis));
        final List<BlockProtection> sqliteProtections = sqLiteStore.loadBlockProtections();
        sqliteProtections.forEach(protection -> LogManager.getLogManager().getLogger("").info(() -> new ProtectionData(
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

    private record ProtectionData(UUID id, String owner, String type, Map<Source, String> accessList, String block, String world, int x, int y, int z) {
    }

    @Override
    public void onDisable() {
        adventure.close();
        HandlerList.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && commands.containsKey(args[0].toLowerCase())) {
            commands.get(args[0].toLowerCase()).execute(sender, new Arguments(Arrays.copyOfRange(args, 1, args.length)));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    public Bolt getBolt() {
        return bolt;
    }

    public BukkitAudiences adventure() {
        return adventure;
    }
}

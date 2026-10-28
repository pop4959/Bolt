package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Mode;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerListener implements Listener {
    private final BoltPlugin plugin;

    public PlayerListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        final UUID uuid = player.getUniqueId();
        final Set<Mode> defaultModes = plugin.defaultModes();
        plugin.getProfileCache().add(uuid, player.getName());
        CompletableFuture.runAsync(() -> {
            final FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(plugin.getDataPath().resolve("players/%s.yml".formatted(uuid)).toFile());
            final BoltPlayer boltPlayer = plugin.getBolt().getBoltPlayer(uuid);
            final boolean hasPersist = playerConfig.getBoolean(Mode.PERSIST.name().toLowerCase(), defaultModes.contains(Mode.PERSIST));
            if (!boltPlayer.hasMode(Mode.PERSIST) && hasPersist) {
                boltPlayer.toggleMode(Mode.PERSIST);
            }
            final boolean hasNoLock = playerConfig.getBoolean(Mode.NOLOCK.name().toLowerCase(), defaultModes.contains(Mode.NOLOCK));
            if (!boltPlayer.hasMode(Mode.NOLOCK) && hasNoLock) {
                boltPlayer.toggleMode(Mode.NOLOCK);
            }
            final boolean hasNoSpam = playerConfig.getBoolean(Mode.NOSPAM.name().toLowerCase(), defaultModes.contains(Mode.NOSPAM));
            if (!boltPlayer.hasMode(Mode.NOSPAM) && hasNoSpam) {
                boltPlayer.toggleMode(Mode.NOSPAM);
            }
            if (hasPersist && !defaultModes.contains(Mode.PERSIST)) {
                SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                        player,
                        Translation.MODE_STILL_ENABLED,
                        Placeholder.component(Translation.Placeholder.MODE, BoltComponents.resolveTranslation("mode_%s".formatted(Mode.PERSIST.name().toLowerCase()), player)),
                        Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt mode persist"))
                ), 20);
            }
        });

    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        plugin.getBolt().removeBoltPlayer(e.getPlayer().getUniqueId());
    }
}

package org.popcraft.bolt.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.popcraft.bolt.BoltPlugin;

public final class PlayerListener implements Listener {
    private final BoltPlugin plugin;

    public PlayerListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        plugin.getProfileCache().add(player.getUniqueId(), player.getName());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        plugin.getBolt().removeBoltPlayer(e.getPlayer().getUniqueId());
    }
}

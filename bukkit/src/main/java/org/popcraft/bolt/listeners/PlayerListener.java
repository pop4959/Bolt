package org.popcraft.bolt.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.popcraft.bolt.BoltPlugin;

@SuppressWarnings("ClassCanBeRecord")
public final class PlayerListener implements Listener {
    private final BoltPlugin plugin;

    public PlayerListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        plugin.getBolt().removePlayerMeta(e.getPlayer().getUniqueId());
    }
}

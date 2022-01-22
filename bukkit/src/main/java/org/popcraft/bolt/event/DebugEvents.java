package org.popcraft.bolt.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.PlayerMeta;

public class DebugEvents implements Listener {
    private final BoltPlugin plugin;

    public DebugEvents(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        if (playerMeta.triggerAction(Action.DEBUG)) {
            final Block clicked = e.getClickedBlock();
            BoltComponents.sendMessage(player, clicked == null ? "null" : plugin.getBolt().getStore().loadBlockProtection(BukkitAdapter.blockLocation(clicked)).toString());
        }
    }
}

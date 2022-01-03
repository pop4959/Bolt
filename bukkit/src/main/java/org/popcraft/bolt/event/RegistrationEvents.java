package org.popcraft.bolt.event;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.popcraft.bolt.Bolt;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.store.Store;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Optional;

public class RegistrationEvents implements Listener {
    private final BoltPlugin plugin;

    public RegistrationEvents(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final Bolt bolt = plugin.getBolt();
        final BoltPlayer boltPlayer = bolt.getBoltPlayer(player.getUniqueId());
        if (boltPlayer.hasAction(Action.LOCK_BLOCK)) {
            final Block clicked = e.getClickedBlock();
            if (clicked == null) {
                return;
            }
            final Store store = bolt.getStore();
            if (store.loadBlockProtection(BukkitAdapter.blockLocation(clicked)).isPresent()) {
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_LOCKED_ALREADY);
            } else {
                store.saveBlockProtection(BukkitAdapter.createPrivateBlockProtection(clicked, player));
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_LOCKED, Strings.toTitleCase(clicked.getType()));
            }
            boltPlayer.removeAction(Action.LOCK_BLOCK);
        } else if (boltPlayer.hasAction(Action.UNLOCK_BLOCK)) {
            final Block clicked = e.getClickedBlock();
            if (clicked == null) {
                return;
            }
            final Store store = bolt.getStore();
            final Optional<BlockProtection> protection = store.loadBlockProtection(BukkitAdapter.blockLocation(clicked));
            if (protection.isPresent()) {
                store.removeBlockProtection(protection.get());
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_UNLOCKED, Strings.toTitleCase(clicked.getType()));
            } else {
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_NOT_LOCKED);
            }
            boltPlayer.removeAction(Action.UNLOCK_BLOCK);
        }
    }
}

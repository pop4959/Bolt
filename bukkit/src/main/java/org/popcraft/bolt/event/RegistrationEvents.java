package org.popcraft.bolt.event;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.protection.BlockProtection;
import org.popcraft.bolt.data.util.BlockLocation;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltPlayer;

import java.util.HashMap;
import java.util.UUID;

public class RegistrationEvents implements Listener {
    private final BoltPlugin plugin;

    public RegistrationEvents(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final BoltPlayer boltPlayer = plugin.getBolt().getBoltPlayer(player.getUniqueId());
        if (boltPlayer.hasAction(Action.LOCK_BLOCK)) {
            final Block clickedBlock = e.getClickedBlock();
            if (clickedBlock == null) {
                player.sendMessage("You didn't click on a block");
                return;
            }
            final BlockLocation blockLocation = new BlockLocation(clickedBlock.getWorld().getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
            plugin.getBolt().getStore().loadBlockProtection(blockLocation).ifPresentOrElse(blockProtection -> player.sendMessage("This block is already locked"), () -> {
                final UUID protectionId = UUID.randomUUID();
                player.getServer().getConsoleSender().sendMessage(protectionId);
                plugin.getBolt().getStore().saveBlockProtection(new BlockProtection(protectionId, player.getUniqueId().toString(), "private", new HashMap<>(), clickedBlock.getType().toString(), clickedBlock.getWorld().getName(), clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ()));
                plugin.adventure().player(player).sendMessage(MiniMessage.get().parse("<gradient:#00FF00:#FF00FF>Successfully locked %s".formatted(clickedBlock.getType().name())));
                player.sendMessage();
                boltPlayer.removeAction(Action.LOCK_BLOCK);
            });
        }
    }
}

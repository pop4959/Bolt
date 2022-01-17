package org.popcraft.bolt.event;

import net.kyori.adventure.text.minimessage.Template;
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
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import java.util.Optional;

import static org.popcraft.bolt.util.lang.Translator.translate;

public class RegistrationEvents implements Listener {
    private final BoltPlugin plugin;

    public RegistrationEvents(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Player player = e.getPlayer();
        final Bolt bolt = plugin.getBolt();
        final PlayerMeta playerMeta = bolt.getPlayerMeta(player.getUniqueId());
        final Block clicked = e.getClickedBlock();
        if (clicked == null) {
            return;
        }
        final Store store = bolt.getStore();
        if (playerMeta.hasAction(Action.LOCK_BLOCK)) {
            if (store.loadBlockProtection(BukkitAdapter.blockLocation(clicked)).isPresent()) {
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_LOCKED_ALREADY);
            } else {
                store.saveBlockProtection(BukkitAdapter.createPrivateBlockProtection(clicked, player));
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_LOCKED, Template.of("block", Strings.toTitleCase(clicked.getType())));
            }
            playerMeta.removeAction(Action.LOCK_BLOCK);
        } else if (playerMeta.hasAction(Action.UNLOCK_BLOCK)) {
            final Optional<BlockProtection> protection = store.loadBlockProtection(BukkitAdapter.blockLocation(clicked));
            if (protection.isPresent()) {
                store.removeBlockProtection(protection.get());
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_UNLOCKED, Template.of("block", Strings.toTitleCase(clicked.getType())));
            } else {
                BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_NOT_LOCKED);
            }
            playerMeta.removeAction(Action.UNLOCK_BLOCK);
        } else if (playerMeta.hasAction(Action.INFO)) {
            store.loadBlockProtection(BukkitAdapter.blockLocation(clicked)).ifPresentOrElse(protection -> BoltComponents.sendMessage(player, Translation.INFO,
                    Template.of("type", Strings.toTitleCase(protection.getType())),
                    Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN)))
            ), () -> BoltComponents.sendMessage(player, Translation.CLICK_BLOCK_NOT_LOCKED));
            playerMeta.removeAction(Action.INFO);
        }
    }
}

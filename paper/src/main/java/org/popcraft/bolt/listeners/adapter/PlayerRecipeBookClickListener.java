package org.popcraft.bolt.listeners.adapter;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

import java.util.function.Consumer;

public record PlayerRecipeBookClickListener(Consumer<PlayerEvent> handler) implements Listener {
    @EventHandler
    public void onPlayerRecipeBookClick(final PlayerRecipeBookClickEvent e) {
        handler.accept(e);
    }
}

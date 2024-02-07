package org.popcraft.bolt.listeners.adapter;

import io.papermc.paper.event.block.BlockPreDispenseEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;

import java.util.function.Consumer;

public record BlockPreDispenseListener(Consumer<BlockEvent> handler) implements Listener {
    @EventHandler
    public void onBlockPreDispense(final BlockPreDispenseEvent e) {
        handler.accept(e);
    }
}

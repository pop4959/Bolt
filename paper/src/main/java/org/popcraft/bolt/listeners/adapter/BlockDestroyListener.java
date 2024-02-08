package org.popcraft.bolt.listeners.adapter;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;

import java.util.function.Consumer;

public record BlockDestroyListener(Consumer<BlockEvent> handler) implements Listener {
    @EventHandler
    public void onBlockDestroy(final BlockDestroyEvent e) {
        handler.accept(e);
    }
}


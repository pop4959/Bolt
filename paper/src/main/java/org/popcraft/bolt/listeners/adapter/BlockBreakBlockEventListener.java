package org.popcraft.bolt.listeners.adapter;

import io.papermc.paper.event.block.BlockBreakBlockEvent;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockEvent;

import java.util.function.BiConsumer;

public record BlockBreakBlockEventListener(BiConsumer<BlockEvent, Block> handler) implements Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakBlock(final BlockBreakBlockEvent e) {
        handler.accept(e, e.getSource());
    }
}

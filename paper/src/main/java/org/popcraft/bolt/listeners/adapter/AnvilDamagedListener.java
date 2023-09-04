package org.popcraft.bolt.listeners.adapter;

import com.destroystokyo.paper.event.block.AnvilDamagedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryEvent;

import java.util.function.Consumer;

public record AnvilDamagedListener(Consumer<InventoryEvent> handler) implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAnvilDamaged(final AnvilDamagedEvent e) {
        if (e.isBreaking()) {
            handler.accept(e);
        }
    }
}

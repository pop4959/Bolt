package org.popcraft.bolt.listeners.adapter;

import io.papermc.paper.event.entity.EntityConstructEvent;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.popcraft.bolt.util.BlockLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record EntityConstructListener(Handler handler) implements Listener {
    public static final Map<BlockLocation, UUID> placedPumpkins = new ConcurrentHashMap<>();

    public static boolean canUse() {
        try {
            Class.forName("io.papermc.paper.event.entity.EntityConstructEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @EventHandler
    public void onEntityConstruct(final EntityConstructEvent e) {
        handler.accept(e, e.getBlocks());
    }

    @FunctionalInterface
    public interface Handler {
        <E extends EntityEvent & Cancellable> void accept(E event, List<Block> blocks);
    }
}

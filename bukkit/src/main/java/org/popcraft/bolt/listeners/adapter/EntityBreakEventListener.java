package org.popcraft.bolt.listeners.adapter;

import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import io.papermc.paper.event.entity.EntityBreakEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public record EntityBreakEventListener(Handler handler, ByEntityHandler byEntityHandler) implements Listener {
  public static boolean canUse() {
    try {
      Class.forName("io.papermc.paper.event.entity.EntityBreakEvent");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @EventHandler
  public void onEntityBreak(final EntityBreakEvent e) {
    if (EntityBreakEvent.RemoveCause.ENTITY.equals(e.getCause())) {
      return;
    }
    handler.accept(e.getEntity(), e);
  }

  @EventHandler
  public void onHangingBreakByEntity(final EntityBreakByEntityEvent e) {
    byEntityHandler.accept(e.getEntity(), e.getRemover(), e);
  }

  @FunctionalInterface
  public interface Handler {
    void accept(Entity entity, Cancellable event);
  }

  @FunctionalInterface
  public interface ByEntityHandler {
    void accept(Entity entity, Entity remover, Cancellable event);
  }
}

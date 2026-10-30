package org.popcraft.bolt.listeners.adapter;

import org.bukkit.ExplosionResult;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public record WindChargeExplosionListener(Handler handler) implements Listener {
  public static boolean canUse() {
    try {
      Class.forName("org.bukkit.ExplosionResult");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @EventHandler
  public void onEntityExplode(final EntityExplodeEvent e) {
    boolean trigger = e.getExplosionResult().equals(ExplosionResult.TRIGGER_BLOCK);
    handler.accept(e, trigger);
  }

  @FunctionalInterface
  public interface Handler {
    void accept(EntityExplodeEvent event, boolean trigger);
  }
}

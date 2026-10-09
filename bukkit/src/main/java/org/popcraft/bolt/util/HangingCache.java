package org.popcraft.bolt.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;

import java.util.Collection;
import java.util.stream.Collectors;

public class HangingCache {
  private final SetMultimap<BlockLocation, Entity> blockSupporting = HashMultimap.create();
  private final SetMultimap<Entity, BlockLocation> entitySupportedBy = HashMultimap.create();

  public void addToCache(Entity entity) {
    if (entity instanceof ItemFrame itemFrame) {
      Block block = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
      BlockLocation support = new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
      blockSupporting.put(support, itemFrame);
      entitySupportedBy.put(itemFrame, support);
    }
  }

  public void removeFromCache(Entity entity) {
    Collection<BlockLocation> supportedBy = entitySupportedBy.get(entity);
    for (BlockLocation block : supportedBy) {
      blockSupporting.remove(block, entity);
    }
    entitySupportedBy.removeAll(entity);
  }

  public Collection<Entity> entitiesSupportedByBlock(Block block) {
    BlockLocation support = new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    return blockSupporting.get(support);
  }

  private String loc(BlockLocation block) { return "%s(%s,%s,%s)".formatted(block.world(), block.x(), block.y(), block.z()); }
  private String entity(Entity e) { return "%s(%s)".formatted(e.getType(), e.getUniqueId()); }

  public void debug() {
    Bukkit.broadcast(Component.text("HangingCache debug:\nsupporting:\n%s\n\nsupported by:\n%s".formatted(
        blockSupporting.asMap().entrySet().stream().map((e) -> "  %s = %s".formatted(loc(e.getKey()), e.getValue().stream().map(this::entity).collect(Collectors.joining(", ")))).collect(Collectors.joining("\n")),
        entitySupportedBy.asMap().entrySet().stream().map((e) -> "  %s = %s".formatted(entity(e.getKey()), e.getValue().stream().map(this::loc).collect(Collectors.joining(", ")))).collect(Collectors.joining("\n"))
    )));
  }
}

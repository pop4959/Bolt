package org.popcraft.bolt.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.popcraft.bolt.BoltPlugin;

import java.util.Collection;
import java.util.Set;

public class HangingCache {
  // Future: Replace with EntityType.CUSHION
  private static final EntityType CUSHION = EnumUtil.valueOf(EntityType.class, "CUSHION").orElse(null);

  private final SetMultimap<BlockLocation, Entity> blockSupporting = HashMultimap.create();
  private final SetMultimap<Entity, BlockLocation> entitySupportedBy = HashMultimap.create();
  private final boolean enabled;

  public HangingCache(BoltPlugin plugin) {
    this.enabled = plugin.getEnableHangingCache();
  }

  public void addToCache(Entity entity) {
    if (!this.enabled) {
      return;
    }
    if (entity instanceof ItemFrame itemFrame) {
      Block block = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
      BlockLocation support = new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
      blockSupporting.put(support, itemFrame);
      entitySupportedBy.put(itemFrame, support);
    } else if (entity instanceof Painting painting) {
      int width = painting.getArt().getBlockWidth();
      int height = painting.getArt().getBlockHeight();
      double halfWidth = (double) width / 2 - 0.5;
      double halfHeight = (double) height / 2 - 0.5;
      Vector forward = painting.getAttachedFace().getDirection();
      // Rotate 90° clockwise
      int rightX = (int) -forward.getZ();
      int rightZ = (int) forward.getX();
      Location bottomLeft = painting.getLocation().add(forward).add(halfWidth * -rightX, -halfHeight, halfWidth * -rightZ);
      String world = bottomLeft.getWorld().getName();

      for (int h = 0; h < width; h++) {
        for (int v = 0; v < height; v++) {
          BlockLocation support = new BlockLocation(world, bottomLeft.getBlockX() + h * rightX, bottomLeft.getBlockY() + v, bottomLeft.getBlockZ() + h * rightZ);
          blockSupporting.put(support, painting);
          entitySupportedBy.put(painting, support);
        }
      }
    } else if (entity.getType().equals(CUSHION)) {
      Location location = entity.getLocation();
      // Cushions may be "in" blocks (i.e. on a slab)
      BlockLocation support = new BlockLocation(location.getWorld().getName(), location.getBlockX(), NumberConversions.ceil(location.getY()) - 1, location.getBlockZ());
      blockSupporting.put(support, entity);
      entitySupportedBy.put(entity, support);
    }
  }

  public void removeFromCache(Entity entity) {
    if (!this.enabled) {
      return;
    }
    Collection<BlockLocation> supportedBy = entitySupportedBy.get(entity);
    for (BlockLocation block : supportedBy) {
      blockSupporting.remove(block, entity);
    }
    entitySupportedBy.removeAll(entity);
  }

  public boolean entityInCache(Entity entity) {
    if (!this.enabled) {
      return false;
    }
    return entitySupportedBy.containsKey(entity);
  }

  public Collection<Entity> entitiesSupportedByBlock(Block block) {
    if (!this.enabled) {
      return Set.of();
    }
    BlockLocation support = new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    return blockSupporting.get(support);
  }
}

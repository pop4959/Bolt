package org.popcraft.bolt.util;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Powerable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.Protection;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RedstoneTracker {
    private static final EnumSet<BlockFace> CARTESIAN_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
    private static final Map<BlockLocation, Press> PRESSES = new ConcurrentHashMap<>();

    private RedstoneTracker() {
    }

    public static void handlePlayerInteract(final PlayerInteractEvent event) {
        if (Event.Result.DENY.equals(event.useInteractedBlock())) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        final Material type = block.getType();
        final Action action = event.getAction();
        final boolean trigger = (Action.RIGHT_CLICK_BLOCK.equals(action) && (Material.LEVER.equals(type) || Tag.BUTTONS.isTagged(type)))
                || (Action.PHYSICAL.equals(action) && Tag.PRESSURE_PLATES.isTagged(type));
        if (trigger) {
            PRESSES.put(new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()), new Press(event.getPlayer().getUniqueId(), type));
        }
    }

    public static void handleBlockRedstone(final BlockRedstoneEvent event) {
        if (event.getNewCurrent() == 0 && !PRESSES.isEmpty()) {
            final Block block = event.getBlock();
            PRESSES.remove(new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        }
    }

    public static boolean allows(final BoltPlugin plugin, final Protection protection, final Block block) {
        if (PRESSES.isEmpty()) {
            return false;
        }
        if (allowsOwnPress(plugin, protection, block)) {
            return true;
        }
        for (final BlockFace face : CARTESIAN_FACES) {
            final Block neighbour = block.getRelative(face);
            if (allowsPress(plugin, protection, neighbour, null)) {
                return true;
            }
            for (final BlockFace attachedFace : CARTESIAN_FACES) {
                if (face.getOppositeFace().equals(attachedFace)) {
                    continue;
                }
                if (allowsPress(plugin, protection, neighbour.getRelative(attachedFace), neighbour)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean allowsOwnPress(final BoltPlugin plugin, final Protection protection, final Block trigger) {
        final Press press = PRESSES.get(new BlockLocation(trigger.getWorld().getName(), trigger.getX(), trigger.getY(), trigger.getZ()));
        return press != null && press.type().equals(trigger.getType()) && plugin.canAccess(protection, press.player(), Permission.REDSTONE);
    }

    private static boolean allowsPress(final BoltPlugin plugin, final Protection protection, final Block trigger, final Block attachedTo) {
        final BlockLocation location = new BlockLocation(trigger.getWorld().getName(), trigger.getX(), trigger.getY(), trigger.getZ());
        final Press press = PRESSES.get(location);
        if (press == null) {
            return false;
        }
        final BlockData data = trigger.getBlockData();
        if (!press.type().equals(trigger.getType()) || !isPowered(data)) {
            PRESSES.remove(location, press);
            return false;
        }
        if (attachedTo != null && !attachedTo(trigger, data).equals(attachedTo)) {
            return false;
        }
        return plugin.canAccess(protection, press.player(), Permission.REDSTONE);
    }

    private static Block attachedTo(final Block trigger, final BlockData data) {
        if (data instanceof final FaceAttachable attachable && data instanceof final Directional directional) {
            return switch (attachable.getAttachedFace()) {
                case FLOOR -> trigger.getRelative(BlockFace.DOWN);
                case CEILING -> trigger.getRelative(BlockFace.UP);
                case WALL -> trigger.getRelative(directional.getFacing().getOppositeFace());
            };
        }
        return trigger.getRelative(BlockFace.DOWN);
    }

    private static boolean isPowered(final BlockData data) {
        if (data instanceof final Powerable powerable) {
            return powerable.isPowered();
        }
        if (data instanceof final AnaloguePowerable analoguePowerable) {
            return analoguePowerable.getPower() > 0;
        }
        return false;
    }

    private record Press(UUID player, Material type) {
    }
}

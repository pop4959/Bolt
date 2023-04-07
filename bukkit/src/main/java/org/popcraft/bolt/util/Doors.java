package org.popcraft.bolt.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.Protection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Doors {
    private static final Map<BlockLocation, Integer> CLOSING = new ConcurrentHashMap<>();

    private Doors() {
    }

    public static void handlePlayerInteract(final BoltPlugin plugin, final Player player, final Block block) {
        final boolean openIron = plugin.isDoorsOpenIron();
        if (!isDoor(block) || !isDoorOpenable(block, openIron)) {
            return;
        }
        final Set<Block> doors = new HashSet<>();
        if (!isDoorOpenableNormally(block)) {
            doors.add(block);
        }
        if (plugin.isDoorsOpenDouble()) {
            final Block hingedBlock = getHingedBlock(block);
            if (hingedBlock != null && isDoor(hingedBlock) && isDoorOpenable(hingedBlock, openIron)) {
                final Protection hungedProtection = plugin.findProtection(hingedBlock).orElse(null);
                if (hungedProtection != null && plugin.canAccess(hungedProtection, player, Permission.INTERACT)) {
                    doors.add(hingedBlock);
                }
            }
        }
        doors.forEach(door -> toggleDoor(door, true));
        final int doorsCloseAfter = plugin.getDoorsCloseAfter();
        if (doorsCloseAfter > 0) {
            doors.add(block);
            doors.forEach(door -> {
                final BlockLocation doorBlockLocation = BukkitAdapter.blockLocation(door);
                CLOSING.compute(doorBlockLocation, ((blockLocation, counter) -> counter == null ? 1 : counter + 1));
                SchedulerUtil.schedule(plugin, player, () -> {
                    final int count = CLOSING.compute(doorBlockLocation, (blockLocation, counter) -> counter == null ? 0 : counter - 1);
                    if (count <= 0) {
                        CLOSING.remove(doorBlockLocation);
                        toggleDoor(door, false);
                    }
                }, doorsCloseAfter * 20L);
            });
        }
    }

    public static boolean isDoor(final Block block) {
        final BlockData blockData = block.getBlockData();
        return blockData instanceof Door || blockData instanceof Gate || blockData instanceof TrapDoor;
    }

    public static Block getHingedBlock(final Block block) {
        final BlockData blockData = block.getBlockData();
        if (!(blockData instanceof final Door door)) {
            return null;
        }
        final BlockFace adjacentFace = switch (door.getFacing()) {
            case NORTH:
                yield door.getHinge() == Door.Hinge.LEFT ? BlockFace.EAST : BlockFace.WEST;
            case SOUTH:
                yield door.getHinge() == Door.Hinge.LEFT ? BlockFace.WEST : BlockFace.EAST;
            case EAST:
                yield door.getHinge() == Door.Hinge.LEFT ? BlockFace.SOUTH : BlockFace.NORTH;
            case WEST:
                yield door.getHinge() == Door.Hinge.LEFT ? BlockFace.NORTH : BlockFace.SOUTH;
            default:
                yield null;
        };
        if (adjacentFace == null) {
            return null;
        }
        return block.getRelative(adjacentFace);
    }

    public static boolean isDoorOpenable(final Block block, final boolean openIron) {
        final Material material = block.getType();
        final Tag<Material> openableDoors = openIron ? Tag.DOORS : Tag.WOODEN_DOORS;
        final Tag<Material> openableGates = Tag.FENCE_GATES;
        final Tag<Material> openableTrapdoors = openIron ? Tag.TRAPDOORS : Tag.WOODEN_TRAPDOORS;
        return openableDoors.isTagged(material) || openableGates.isTagged(material) || openableTrapdoors.isTagged(material);
    }

    public static boolean isDoorOpenableNormally(final Block block) {
        final Material material = block.getType();
        final boolean isIronDoor = Tag.DOORS.isTagged(material) && !Tag.WOODEN_DOORS.isTagged(material);
        final boolean isIronTrapdoor = Tag.TRAPDOORS.isTagged(material) && !Tag.WOODEN_TRAPDOORS.isTagged(material);
        return !isIronDoor && !isIronTrapdoor;
    }

    public static void toggleDoor(final Block block, final boolean canOpen) {
        if (block.getBlockData() instanceof final Openable openable) {
            if (!canOpen && !openable.isOpen()) {
                return;
            }
            openable.setOpen(!openable.isOpen());
            block.setBlockData(openable);
            playDoorSound(block, openable.isOpen());
        }
    }

    private static void playDoorSound(final Block block, final boolean open) {
        final Material type = block.getType();
        final Sound sound;
        if (Tag.DOORS.isTagged(type)) {
            if (Tag.WOODEN_DOORS.isTagged(type)) {
                sound = open ? Sound.BLOCK_WOODEN_DOOR_OPEN : Sound.BLOCK_WOODEN_DOOR_CLOSE;
            } else {
                sound = open ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE;
            }
        } else if (Tag.TRAPDOORS.isTagged(type)) {
            if (Tag.WOODEN_TRAPDOORS.isTagged(type)) {
                sound = open ? Sound.BLOCK_WOODEN_TRAPDOOR_OPEN : Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE;
            } else {
                sound = open ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE;
            }
        } else if (Tag.FENCE_GATES.isTagged(type)) {
            sound = open ? Sound.BLOCK_FENCE_GATE_OPEN : Sound.BLOCK_FENCE_GATE_CLOSE;
        } else {
            return;
        }
        final World world = block.getWorld();
        final Location location = block.getLocation();
        world.playSound(location, sound, 1, 1);
    }
}

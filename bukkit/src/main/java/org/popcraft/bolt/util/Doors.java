package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceTypeResolver;
import org.popcraft.bolt.source.SourceTypes;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Doors {
    private static final SourceResolver DOOR_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.DOOR));
    private static final Map<BlockLocation, Integer> CLOSING = new ConcurrentHashMap<>();
    private static final Set<PlayerInteractEvent> SELF_FIRED_EVENTS = ConcurrentHashMap.newKeySet();
    // Future: Replace with Tag.MOB_INTERACTABLE_DOORS
    private static final Tag<Material> MOB_INTERACTABLE_DOORS = Bukkit.getServer().getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft("mob_interactable_doors"), Material.class);
    // There is no tag for copper doors
    private static final Function<Material, Boolean> IS_COPPER_DOOR = (material) -> material.name().contains("COPPER_DOOR") || material.name().contains("COPPER_TRAPDOOR");

    private Doors() {
    }

    public static void handlePlayerInteract(final BoltPlugin plugin, final PlayerInteractEvent event) {
        if (SELF_FIRED_EVENTS.remove(event)) {
            return;
        }

        final boolean openIron = plugin.isDoorsOpenIron();
        final Block block = event.getClickedBlock();
        if (block == null || !isDoor(block) || !isDoorOpenable(block, openIron) || interactionDenied(plugin, event)) {
            return;
        }
        final Player player = event.getPlayer();
        final Set<Block> doors = new HashSet<>();
        if (!isDoorOpenableNormally(block)) {
            doors.add(block);
        }
        if (plugin.isDoorsOpenDouble()) {
            final Block hingedBlock = getHingedBlock(block);
            if (hingedBlock != null && areMatchingDoors(block, hingedBlock) && isDoor(hingedBlock) && isDoorOpenable(hingedBlock, openIron)) {
                // Don't open matching door when left-clicking on a wooden door
                if (event.getAction() != Action.LEFT_CLICK_BLOCK || !isDoorOpenableNormally(hingedBlock)) {
                    final Protection hingedProtection = plugin.findProtection(hingedBlock);
                    if (hingedProtection != null && plugin.canAccess(hingedProtection, player, Permission.INTERACT)) {
                        doors.add(hingedBlock);
                    }
                }
            }
        }
        doors.forEach(door -> toggleDoor(plugin, event, door, true));
        final int doorsCloseAfter = plugin.getDoorsCloseAfter();
        if (doorsCloseAfter > 0) {
            doors.add(block);
            doors.forEach(door -> {
                final Protection protection = plugin.findProtection(door);
                if (protection == null || (!plugin.canAccess(protection, player.getUniqueId(), Permission.AUTO_CLOSE) && !plugin.canAccess(protection, DOOR_SOURCE_RESOLVER, Permission.AUTO_CLOSE))) {
                    return;
                }
                final BlockLocation doorBlockLocation = new BlockLocation(door.getWorld().getName(), door.getX(), door.getY(), door.getZ());
                CLOSING.compute(doorBlockLocation, ((blockLocation, counter) -> counter == null ? 1 : counter + 1));
                SchedulerUtil.schedule(plugin, player, () -> {
                    final int count = CLOSING.compute(doorBlockLocation, (blockLocation, counter) -> counter == null ? 0 : counter - 1);
                    if (count <= 0) {
                        CLOSING.remove(doorBlockLocation);
                        toggleDoor(plugin, event, door, false);
                    }
                }, doorsCloseAfter * 20L);
            });
        }
    }

    public static boolean interactionDenied(final BoltPlugin plugin, final PlayerInteractEvent event) {
        if (!plugin.getDoorsFixPlugins()) {
            return false;
        }

        final Block block = event.getClickedBlock();
        if (block == null) {
            return false;
        }

        if (event.useInteractedBlock().equals(Event.Result.DENY)) {
            return true;
        }

        final boolean leftClick = org.bukkit.event.block.Action.LEFT_CLICK_BLOCK.equals(event.getAction());
        final boolean ironDoor = plugin.isDoorsOpenIron() && !isDoorOpenableNormally(block);

        if (leftClick || ironDoor) {
            final BlockState originalState = block.getState();
            if (ironDoor) {
                block.setType(Material.OAK_DOOR, false);
            }
            final PlayerInteractEvent fakeInteract = new PlayerInteractEvent(
                    event.getPlayer(),
                    org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                    event.getItem(),
                    event.getClickedBlock(),
                    event.getBlockFace()
            );
            SELF_FIRED_EVENTS.add(fakeInteract);
            SchedulerUtil.schedule(plugin, block.getLocation(), () -> SELF_FIRED_EVENTS.remove(fakeInteract));
            plugin.getServer().getPluginManager().callEvent(fakeInteract);
            if (ironDoor) {
                block.setBlockData(originalState.getBlockData(), false);
            }
            return fakeInteract.useInteractedBlock().equals(Event.Result.DENY);
        }

        return false;
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

    public static boolean areMatchingDoors(final Block door, final Block hinged) {
        if (IS_COPPER_DOOR.apply(door.getType()) && IS_COPPER_DOOR.apply(hinged.getType())) {
            // Should match regardless of oxidation level
            return true;
        }
        return hinged.getType().equals(door.getType());
    }

    public static boolean isDoorOpenable(final Block block, final boolean openIron) {
        final Material material = block.getType();
        if (Material.IRON_DOOR.equals(material) || Material.IRON_TRAPDOOR.equals(material)) {
            return openIron;
        }
        if (Tag.DOORS.isTagged(material) || Tag.FENCE_GATES.isTagged(material) || Tag.TRAPDOORS.isTagged(material)) {
            return true;
        }
        return MOB_INTERACTABLE_DOORS != null && MOB_INTERACTABLE_DOORS.isTagged(material);
    }

    public static boolean isDoorOpenableNormally(final Block block) {
        return isDoorOpenable(block, false);
    }

    public static void toggleDoor(final BoltPlugin plugin, final PlayerInteractEvent event, final Block block, final boolean canOpen) {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (Event.Result.DENY.equals(event.useInteractedBlock())) {
                return;
            }
            if (!(block.getBlockData() instanceof final Openable openable)) {
                return;
            }
            if (!canOpen && !openable.isOpen()) {
                return;
            }
            openable.setOpen(!openable.isOpen());
            block.setBlockData(openable);
            playDoorSound(block, openable.isOpen());
        });
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

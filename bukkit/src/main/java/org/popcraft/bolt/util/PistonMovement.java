package org.popcraft.bolt.util;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.PistonMoveReaction;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceTypeResolver;
import org.popcraft.bolt.source.SourceTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PistonMovement {
    private static final SourceResolver REDSTONE_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.REDSTONE));

    private PistonMovement() {
    }

    public static boolean allowed(final BoltPlugin plugin, final Block piston, final List<Block> blocks, final BlockFace direction, final String permission) {
        if (blocks.stream().noneMatch(plugin::isProtected)) {
            return true;
        }
        final Protection pistonProtection = plugin.loadProtection(piston);
        if (pistonProtection == null || !plugin.canAccess(pistonProtection, REDSTONE_SOURCE_RESOLVER, Permission.REDSTONE)) {
            return false;
        }
        final UUID owner = pistonProtection.getOwner();
        for (final Block block : blocks) {
            if (PistonMoveReaction.BREAK.equals(block.getPistonMoveReaction())) {
                final Protection protection = plugin.findProtection(block);
                if (protection != null && !plugin.canAccess(protection, owner, Permission.DESTROY)) {
                    return false;
                }
                continue;
            }
            final BlockProtection protection = plugin.loadProtection(block);
            if (protection != null && !plugin.canAccess(protection, owner, permission)) {
                return false;
            }
            if (plugin.matchProtection(block) != null) {
                return false;
            }
            final Block destination = block.getRelative(direction);
            if (!blocks.contains(destination) && plugin.loadProtection(destination) != null) {
                return false;
            }
        }
        return true;
    }

    public static void relocate(final BoltPlugin plugin, final List<Block> blocks, final BlockFace direction) {
        final List<Moved> moved = new ArrayList<>();
        final List<Block> breaking = new ArrayList<>();
        final Set<UUID> movedIds = new HashSet<>();
        for (final Block block : blocks) {
            if (PistonMoveReaction.BREAK.equals(block.getPistonMoveReaction())) {
                breaking.add(block);
                continue;
            }
            final BlockProtection protection = plugin.loadProtection(block);
            if (protection != null) {
                moved.add(new Moved(protection, block.getRelative(direction)));
                movedIds.add(protection.getId());
            }
        }
        final Set<UUID> brokenIds = new HashSet<>();
        final List<Protection> broken = new ArrayList<>();
        for (final Block block : breaking) {
            final Protection protection = plugin.findProtection(block);
            if (protection != null && !movedIds.contains(protection.getId()) && brokenIds.add(protection.getId())) {
                broken.add(protection);
            }
        }
        for (final Moved entry : moved) {
            final BlockProtection existing = plugin.loadProtection(entry.destination());
            if (existing != null && !movedIds.contains(existing.getId()) && !brokenIds.contains(existing.getId())) {
                return;
            }
        }
        broken.forEach(plugin::removeProtection);
        final List<BlockProtection> protections = new ArrayList<>(moved.size());
        for (final Moved entry : moved) {
            final Block destination = entry.destination();
            final BlockProtection protection = entry.protection();
            protection.setWorld(destination.getWorld().getName());
            protection.setX(destination.getX());
            protection.setY(destination.getY());
            protection.setZ(destination.getZ());
            protections.add(protection);
        }
        plugin.getBolt().getStore().saveBlockProtections(protections);
    }

    private record Moved(BlockProtection protection, Block destination) {
    }
}

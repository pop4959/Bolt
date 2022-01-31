package org.popcraft.bolt.util.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.popcraft.bolt.util.matcher.Match;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

public class DoubleChestMatcher extends BlockMatcher {
    private static final EnumSet<Material> CHESTS = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST);

    @Override
    public boolean canMatch(Block block) {
        return CHESTS.contains(block.getType());
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (block.getBlockData() instanceof final Chest chest && !Chest.Type.SINGLE.equals(chest.getType())) {
            final BlockFace adjacentFace = switch (chest.getFacing()) {
                case NORTH:
                    yield chest.getType() == Chest.Type.LEFT ? BlockFace.EAST : BlockFace.WEST;
                case SOUTH:
                    yield chest.getType() == Chest.Type.LEFT ? BlockFace.WEST : BlockFace.EAST;
                case EAST:
                    yield chest.getType() == Chest.Type.LEFT ? BlockFace.SOUTH : BlockFace.NORTH;
                case WEST:
                    yield chest.getType() == Chest.Type.LEFT ? BlockFace.NORTH : BlockFace.SOUTH;
                default:
                    yield null;
            };
            if (adjacentFace != null) {
                final Block adjacent = block.getRelative(adjacentFace);
                if (block.getType().equals(adjacent.getType())) {
                    return Optional.of(Match.ofBlocks(Collections.singleton(adjacent)));
                }
            }
        }
        return Optional.empty();
    }
}

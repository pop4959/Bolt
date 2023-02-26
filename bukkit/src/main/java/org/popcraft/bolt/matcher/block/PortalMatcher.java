package org.popcraft.bolt.matcher.block;

import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PortalMatcher implements BlockMatcher {
    private static final EnumSet<BlockFace> CARTESIAN_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
    private static final EnumSet<BlockFace> X_FACES = EnumSet.of(BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
    private static final EnumSet<BlockFace> Z_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP, BlockFace.DOWN);
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.contains(Material.NETHER_PORTAL);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean canMatch(Block block) {
        return enabled && (Material.OBSIDIAN.equals(block.getType()) || Material.NETHER_PORTAL.equals(block.getType()));
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        final Set<Block> blocks = new HashSet<>();
        for (final BlockFace blockFace : CARTESIAN_FACES) {
            final Block adjacent = block.getRelative(blockFace);
            if (Material.NETHER_PORTAL.equals(adjacent.getType()) && adjacent.getBlockData() instanceof final Orientable orientable) {
                blocks.add(adjacent);
                findPortal(Axis.X.equals(orientable.getAxis()) ? X_FACES : Z_FACES, blocks, adjacent);
            }
        }
        return Optional.of(Match.ofBlocks(blocks));
    }

    private void findPortal(final EnumSet<BlockFace> faces, final Set<Block> found, final Block current) {
        for (final BlockFace blockFace : faces) {
            final Block adjacent = current.getRelative(blockFace);
            if (Material.NETHER_PORTAL.equals(adjacent.getType()) && !found.contains(adjacent)) {
                found.add(adjacent);
                findPortal(faces, found, adjacent);
            }
        }
    }
}

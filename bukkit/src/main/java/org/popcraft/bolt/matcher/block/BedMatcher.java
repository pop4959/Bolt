package org.popcraft.bolt.matcher.block;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.Optional;

public class BedMatcher implements BlockMatcher {
    @Override
    public boolean canMatch(Block block) {
        return Tag.BEDS.isTagged(block.getType());
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (block.getBlockData() instanceof final Bed bed) {
            if (Bed.Part.FOOT.equals(bed.getPart())) {
                final Block head = block.getRelative(bed.getFacing());
                if (Tag.BEDS.isTagged(head.getType())) {
                    return Optional.of(Match.ofBlocks(Collections.singleton(head)));
                }
            } else {
                final Block foot = block.getRelative(bed.getFacing().getOppositeFace());
                if (Tag.BEDS.isTagged(foot.getType())) {
                    return Optional.of(Match.ofBlocks(Collections.singleton(foot)));
                }
            }
        }
        return Optional.empty();
    }
}

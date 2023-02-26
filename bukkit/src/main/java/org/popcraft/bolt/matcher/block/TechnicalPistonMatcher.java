package org.popcraft.bolt.matcher.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.TechnicalPiston;
import org.bukkit.entity.EntityType;
import org.popcraft.bolt.matcher.Match;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class TechnicalPistonMatcher implements BlockMatcher {
    private boolean enabled;

    @Override
    public void initialize(Set<Material> protectableBlocks, Set<EntityType> protectableEntities) {
        enabled = protectableBlocks.stream().anyMatch(material -> material.createBlockData() instanceof Piston);
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean canMatch(Block block) {
        return enabled && Material.PISTON_HEAD.equals(block.getType());
    }

    @Override
    public Optional<Match> findMatch(Block block) {
        if (block.getBlockData() instanceof final TechnicalPiston technicalPiston) {
            return Optional.of(Match.ofBlocks(Collections.singleton(block.getRelative(technicalPiston.getFacing().getOppositeFace()))));
        }
        return Optional.empty();
    }
}

package org.popcraft.bolt;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.SourceResolver;

import java.util.UUID;

public interface BoltAPI {
    boolean isProtectable(final Block block);

    boolean isProtectable(final Entity entity);

    boolean isProtected(final Block block);

    boolean isProtected(final Entity entity);

    BlockProtection loadProtection(final Block block);

    EntityProtection loadProtection(final Entity entity);

    void saveProtection(final Protection protection);

    void removeProtection(final Protection protection);

    Protection findProtection(final Block block);

    Protection findProtection(final Entity entity);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean canAccess(final Block block, final Player player, final String... permissions);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean canAccess(final Entity entity, final Player player, final String... permissions);

    boolean canAccess(final Protection protection, final Player player, final String... permissions);

    boolean canAccess(final Protection protection, final UUID uuid, final String... permissions);

    boolean canAccess(final Protection protection, final SourceResolver sourceResolver, String... permissions);
}

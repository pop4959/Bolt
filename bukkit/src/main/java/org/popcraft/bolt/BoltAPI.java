package org.popcraft.bolt;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.PlayerSourceResolver;
import org.popcraft.bolt.source.SourceResolver;

import java.util.Collection;
import java.util.UUID;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public interface BoltAPI {
    boolean isProtectable(final Block block);

    boolean isProtectable(final Entity entity);

    boolean isProtected(final Block block);

    boolean isProtected(final Entity entity);

    boolean isProtectedExact(final Block block);

    boolean isProtectedExact(final Entity entity);

    BlockProtection createProtection(final Block block, final UUID owner, final String type);

    EntityProtection createProtection(final Entity entity, final UUID owner, final String type);

    Collection<Protection> loadProtections();

    BlockProtection loadProtection(final Block block);

    EntityProtection loadProtection(final Entity entity);

    void saveProtection(final Protection protection);

    void removeProtection(final Protection protection);

    Protection findProtection(final Block block);

    Protection findProtection(final Entity entity);

    Collection<Protection> findProtections(final World world, final BoundingBox boundingBox);

    boolean canAccess(final Block block, final Player player, final String... permissions);

    boolean canAccess(final Entity entity, final Player player, final String... permissions);

    boolean canAccess(final Protection protection, final Player player, final String... permissions);

    boolean canAccess(final Protection protection, final UUID uuid, final String... permissions);

    boolean canAccess(final Protection protection, final SourceResolver sourceResolver, final String... permissions);

    void registerPlayerSourceResolver(final PlayerSourceResolver playerSourceResolver);
}

package org.popcraft.bolt.protection;

import org.bukkit.block.Block;

/**
 * Decides whether custom blocks can be protected, and with which settings. Register it with
 * {@link org.popcraft.bolt.BoltAPI#registerProtectableTargetResolver(ProtectableTargetResolver)}.
 * <p>
 * Resolvers are called on the thread handling the event (the region thread on Folia) and must be fast and must not
 * block.
 */
@FunctionalInterface
public interface ProtectableTargetResolver {
    ProtectableResult resolve(final Block block);
}

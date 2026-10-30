package org.popcraft.bolt.protection;

import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.Optional;

/**
 * Supplies display names for custom blocks and block protections in Bolt messages. Register it with
 * {@link org.popcraft.bolt.BoltAPI#registerProtectionNameResolver(ProtectionNameResolver)}.
 * <p>
 * Return {@link Optional#empty()} for anything the resolver does not recognize. Resolvers must be fast and must not
 * block.
 */
public interface ProtectionNameResolver {
    default Optional<Component> resolve(final Block block, final CommandSender viewer) {
        return Optional.empty();
    }

    /**
     * Resolves the name of a stored block protection. The protected block may not be loaded.
     */
    default Optional<Component> resolve(final BlockProtection protection, final CommandSender viewer) {
        return Optional.empty();
    }
}

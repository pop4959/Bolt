package org.popcraft.bolt.protection;

import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ProtectableResolverRegistry {
    private final List<ProtectableTargetResolver> targetResolvers = new ArrayList<>();
    private final List<ProtectionNameResolver> nameResolvers = new ArrayList<>();

    public void registerTargetResolver(final ProtectableTargetResolver resolver) {
        targetResolvers.add(resolver);
    }

    public void registerNameResolver(final ProtectionNameResolver resolver) {
        nameResolvers.add(resolver);
    }

    public ProtectableResult resolve(final Block block) {
        for (final ProtectableTargetResolver resolver : targetResolvers) {
            final ProtectableResult result;
            try {
                result = resolver.resolve(block);
            } catch (final RuntimeException e) {
                e.printStackTrace();
                continue;
            }
            if (result != null && !result.isPass()) {
                return result;
            }
        }
        return ProtectableResult.pass();
    }

    public Optional<Component> resolveName(final Block block, final CommandSender viewer) {
        return resolveName(resolver -> resolver.resolve(block, viewer));
    }

    public Optional<Component> resolveName(final BlockProtection protection, final CommandSender viewer) {
        return resolveName(resolver -> resolver.resolve(protection, viewer));
    }

    private Optional<Component> resolveName(final Function<ProtectionNameResolver, Optional<Component>> call) {
        for (final ProtectionNameResolver resolver : nameResolvers) {
            final Optional<Component> result;
            try {
                result = call.apply(resolver);
            } catch (final RuntimeException e) {
                e.printStackTrace();
                continue;
            }
            if (result != null && result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}

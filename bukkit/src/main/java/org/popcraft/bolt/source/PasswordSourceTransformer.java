package org.popcraft.bolt.source;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.concurrent.CompletableFuture;

public class PasswordSourceTransformer implements SourceTransformer {
    @Override
    public CompletableFuture<String> transformIdentifier(String identifier, CommandSender sender) {
        final Source password = Source.password(identifier);
        return CompletableFuture.completedFuture(password != null ? password.getIdentifier() : null);
    }
}

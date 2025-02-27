package org.popcraft.bolt.source;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SourceTransformer {
    default CompletableFuture<String> transformIdentifier(String identifier, CommandSender sender) {
        return CompletableFuture.completedFuture(identifier);
    }

    default List<String> completions(CommandSender sender) {
        return List.of();
    }
}

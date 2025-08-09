package org.popcraft.bolt.source;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Allows for certain source types to be stored differently from how the user interacts with them through commands.
 * For example, a player source can be stored using the player's UUID, but the user can use usernames in commands, and
 * have usernames displayed to them.
 * <p>
 * Also allows for providing TAB completion suggestions. For example, a player source can suggest the name of currently
 * online players.
 */
public interface SourceTransformer {
    /**
     * Converts a user's input into the stored format
     *
     * @param identifier user input
     * @return identifier to store in a future. The future may contain {@code null} if an invalid input was given.
     */
    default CompletableFuture<String> transformIdentifier(String identifier) {
        return CompletableFuture.completedFuture(identifier);
    }

    /**
     * Reports an error to the sender. This may be called by a command when {@link #transformIdentifier(String)} returns
     * {@code null}.
     *
     * @param identifier user input which failed to be transformed.
     * @param sender     the sender to provide feedback to
     */
    default void errorNotFound(String identifier, CommandSender sender) {
        BoltComponents.sendMessage(
                sender,
                Translation.GENERIC_NOT_FOUND,
                Placeholder.component(Translation.Placeholder.X, Component.text(identifier))
        );
    }

    /**
     * TAB completion suggestions for this source type.
     *
     * @param sender the user making the input
     * @return list of TAB completions
     */
    default List<String> completions(CommandSender sender) {
        return List.of();
    }

    /**
     * Converts a stored identifier back into what should be displayed to the user.
     *
     * @param identifier stored format
     * @return what to display
     */
    default String unTransformIdentifier(String identifier) {
        return identifier;
    }

    /**
     * A source transformer that makes no changes and provides no suggestions.
     */
    SourceTransformer DEFAULT = new SourceTransformer() {
    };
}

package org.popcraft.bolt.source;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerSourceTransformer implements SourceTransformer {
    private final BoltPlugin plugin;

    public PlayerSourceTransformer(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<String> transformIdentifier(String identifier) {
        return Profiles.findOrLookupProfileByName(identifier).thenApply(profile -> {
            if (profile.uuid() != null) {
                return profile.uuid().toString();
            }
            return null;
        });
    }

    @Override
    public void errorNotFound(String identifier, CommandSender sender) {
        BoltComponents.sendMessage(
                sender,
                Translation.PLAYER_NOT_FOUND,
                Placeholder.component(Translation.Placeholder.PLAYER, Component.text(identifier))
        );
    }

    @Override
    public List<String> completions(CommandSender sender) {
        List<String> list = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            list.add(player.getName());
        }
        return list;
    }

    @Override
    public String unTransformIdentifier(String identifier) {
        final UUID uuid = UUID.fromString(identifier);
        final String playerName = Profiles.findProfileByUniqueId(uuid).name();
        return Optional.ofNullable(playerName).orElse(identifier);
    }
}

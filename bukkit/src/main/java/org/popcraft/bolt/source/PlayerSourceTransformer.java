package org.popcraft.bolt.source;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerSourceTransformer implements SourceTransformer {
    private final BoltPlugin plugin;

    public PlayerSourceTransformer(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<String> transformIdentifier(String identifier, CommandSender sender) {
        return Profiles.findOrLookupProfileByName(identifier).thenApply(profile -> {
            if (profile.uuid() != null) {
                return profile.uuid().toString();
            } else {
                SchedulerUtil.schedule(plugin, sender, () -> BoltComponents.sendMessage(
                        sender,
                        Translation.PLAYER_NOT_FOUND,
                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(identifier))
                ));
                return null;
            }
        });
    }

    @Override
    public List<String> completions(CommandSender sender) {
        List<String> list = new ArrayList<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            list.add(player.getName());
        }
        return list;
    }
}

package org.popcraft.bolt.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import static org.popcraft.bolt.lang.Translator.translate;

public final class BoltComponents {
    private static MiniMessage miniMessage;
    private static BukkitAudiences adventure;

    private BoltComponents() {
    }

    public static void enable(final Plugin plugin) {
        if (adventure == null) {
            adventure = BukkitAudiences.create(plugin);
            miniMessage = MiniMessage.miniMessage();
        }
    }

    public static void disable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
            miniMessage = null;
        }
    }

    public static void sendMessage(final CommandSender sender, String key, TagResolver... placeholders) {
        adventure.sender(sender).sendMessage(miniMessage.deserialize(translate(key), placeholders));
    }
}

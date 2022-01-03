package org.popcraft.bolt.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import static org.popcraft.bolt.util.lang.Translator.translate;

public final class BoltComponents {
    private static MiniMessage miniMessage;
    private static BukkitAudiences adventure;

    private BoltComponents() {
    }

    public static void enable(final Plugin plugin) {
        if (adventure == null) {
            adventure = BukkitAudiences.create(plugin);
            miniMessage = MiniMessage.get();
        }
    }

    public static void disable() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
            miniMessage = null;
        }
    }

    public static void sendMessage(final CommandSender sender, String key, Template... placeholders) {
        adventure.sender(sender).sendMessage(miniMessage.parse(translate(key), placeholders));
    }
}

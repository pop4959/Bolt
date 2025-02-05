package org.popcraft.bolt.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.popcraft.bolt.lang.Translator;

import java.util.Locale;

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

    private static void sendMessage(final CommandSender sender, final Component component) {
        if (!component.equals(Component.empty())) {
            adventure.sender(sender).sendMessage(component);
        }
    }

    public static void sendMessage(final CommandSender sender, String key, TagResolver... placeholders) {
        sendMessage(sender, resolveTranslation(key, sender, placeholders));
    }

    public static void sendMessage(final CommandSender sender, String key, boolean actionBar, TagResolver... placeholders) {
        if (actionBar) {
            adventure.sender(sender).sendActionBar(resolveTranslation(key, sender, placeholders));
        } else {
            sendMessage(sender, resolveTranslation(key, sender, placeholders));
        }
    }

    public static void sendClickableMessage(final CommandSender sender, String key, ClickEvent clickEvent, TagResolver... placeholders) {
        sendMessage(sender, resolveTranslation(key, sender, placeholders).clickEvent(clickEvent));
    }

    public static Component resolveTranslation(final String key, final CommandSender sender, TagResolver... placeholders) {
        return miniMessage.deserialize(translateRaw(key, sender), placeholders);
    }

    /**
     * Translate a message with the given sender's locale. This method returns a string, usually a MiniMessage string,
     * so it doesn't have formatting applied. See {@link #resolveTranslation(String, CommandSender, TagResolver...)}
     * for the method which returns a Component.
     * @param key translation key
     * @param sender player to use the locale of
     * @return localized string
     */
    public static String translateRaw(final String key, final CommandSender sender) {
        return translate(key, getLocaleOf(sender));
    }

    /**
     * Fetch the locale of a {@link CommandSender}. This will use the player's configured locale if the command sender
     * is a {@link Player}, otherwise, it will fall back to an empty locale. The empty locale is translated to the
     * language configured in the plugin config as the default language.
     * @param sender command sender to check the locale of
     * @return a locale, possibly an empty one
     */
    public static Locale getLocaleOf(CommandSender sender) {
        if (sender instanceof Player player) {
            return Translator.parseLocale(player.getLocale());
        } else {
            return new Locale("");
        }
    }
}

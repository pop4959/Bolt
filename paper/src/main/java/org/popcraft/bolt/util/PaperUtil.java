package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class PaperUtil {
    private static boolean getOfflinePlayerIfCachedExists;

    static {
        try {
            Bukkit.class.getMethod("getOfflinePlayerIfCached", String.class);
            getOfflinePlayerIfCachedExists = true;
        } catch (NoSuchMethodException e) {
            getOfflinePlayerIfCachedExists = false;
        }
    }

    private PaperUtil() {
    }

    public static OfflinePlayer getOfflinePlayer(final String name) {
        if (getOfflinePlayerIfCachedExists) {
            return Bukkit.getOfflinePlayerIfCached(name);
        } else {
            return Bukkit.getOfflinePlayer(name);
        }
    }
}

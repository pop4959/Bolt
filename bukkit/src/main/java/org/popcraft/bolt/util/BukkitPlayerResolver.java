package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BukkitPlayerResolver implements SourceResolver {
    private final BoltPlayer boltPlayer;

    public BukkitPlayerResolver(final BoltPlayer boltPlayer) {
        this.boltPlayer = boltPlayer;
    }

    @Override
    public boolean resolve(Source source) {
        if (boltPlayer.sources().contains(source)) {
            return true;
        }
        final Player player = Bukkit.getPlayer(boltPlayer.getUuid());
        return player != null && SourceType.PERMISSION.equals(source.getType()) && player.hasPermission(source.getIdentifier());
    }
}

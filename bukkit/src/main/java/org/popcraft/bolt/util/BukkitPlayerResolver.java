package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.popcraft.bolt.Bolt;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceType;

import java.util.UUID;

public class BukkitPlayerResolver implements SourceResolver {
    private final Bolt bolt;
    private final UUID uuid;
    private final BoltPlayer boltPlayer;
    private final Player player;

    public BukkitPlayerResolver(final Bolt bolt, final UUID uuid) {
        this.bolt = bolt;
        this.uuid = uuid;
        this.boltPlayer = bolt.getBoltPlayer(uuid);
        this.player = Bukkit.getPlayer(boltPlayer.getUuid());
    }

    @Override
    public boolean resolve(Source source) {
        if (boltPlayer.sources().contains(source)) {
            return true;
        }
        if (SourceType.GROUP.equals(source.getType())) {
            final Group group = bolt.getStore().loadGroup(source.getIdentifier()).join();
            if (group != null && group.getMembers().contains(uuid)) {
                return true;
            }
        }
        return player != null && SourceType.PERMISSION.equals(source.getType()) && player.hasPermission(source.getIdentifier());
    }
}

package org.popcraft.bolt;

import org.bukkit.entity.Player;
import org.popcraft.bolt.data.Source;
import org.popcraft.bolt.data.defaults.DefaultSourceType;
import org.popcraft.bolt.data.protection.Protection;
import org.popcraft.bolt.registry.AccessRegistry;

public class AccessManager {
    private final Bolt bolt;

    public AccessManager(final Bolt bolt) {
        this.bolt = bolt;
    }

    public boolean hasAccess(final Player player, final Protection protection, String permission) {
        if (player.getUniqueId().toString().equals(protection.getOwner())) {
            return true;
        }
        final AccessRegistry accessRegistry = bolt.getAccessRegistry();
        if (accessRegistry.getAccess(protection.getType()).map(access -> access.permissions().contains(permission)).orElse(false)) {
            return true;
        }
        final Source playerSource = new Source(DefaultSourceType.PLAYER, player.getUniqueId().toString());
        if (protection.getAccessList().containsKey(playerSource)) {
            final String accessType = protection.getAccessList().get(playerSource);
            return accessRegistry.getAccess(accessType).map(access -> access.permissions().contains(permission)).orElse(false);
        }
        return false;
    }
}

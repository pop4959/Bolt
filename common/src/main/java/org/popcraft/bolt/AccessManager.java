package org.popcraft.bolt;

import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.util.defaults.DefaultSource;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.PlayerMeta;

public class AccessManager {
    private final Bolt bolt;

    public AccessManager(final Bolt bolt) {
        this.bolt = bolt;
    }

    public boolean hasAccess(final PlayerMeta player, final Protection protection, String permission) {
        if (player.getUuid().equals(protection.getOwner())) {
            return true;
        }
        final AccessRegistry accessRegistry = bolt.getAccessRegistry();
        if (accessRegistry.getAccess(protection.getType()).map(access -> access.permissions().contains(permission)).orElse(false)) {
            return true;
        }
        final Source playerSource = new Source(DefaultSource.PLAYER.getType(), player.getUuid().toString());
        if (protection.getAccessList().containsKey(playerSource)) {
            final String accessType = protection.getAccessList().get(playerSource);
            return accessRegistry.getAccess(accessType).map(access -> access.permissions().contains(permission)).orElse(false);
        }
        return false;
    }
}

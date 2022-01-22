package org.popcraft.bolt;

import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.util.defaults.DefaultSource;

import java.util.Map;

public record AccessManager(Bolt bolt) {
    public boolean hasAccess(final PlayerMeta player, final Protection protection, String... permissions) {
        if (player.getUuid().equals(protection.getOwner())) {
            return true;
        }
        final AccessRegistry accessRegistry = bolt.getAccessRegistry();
        final boolean[] results = new boolean[permissions.length];
        accessRegistry.get(protection.getType()).ifPresent(access -> {
            for (int i = 0; i < permissions.length; ++i) {
                if (access.permissions().contains(permissions[i])) {
                    results[i] = true;
                }
            }
        });
        // TODO: Improve this to allow matching any source, not just players
        final Source playerSource = new Source(DefaultSource.PLAYER.getType(), player.getUuid().toString());
        final Map<Source, String> accessList = protection.getAccessList();
        if (accessList.containsKey(playerSource)) {
            accessRegistry.get(accessList.get(playerSource)).ifPresent(access -> {
                for (int i = 0; i < permissions.length; ++i) {
                    if (access.permissions().contains(permissions[i])) {
                        results[i] = true;
                    }
                }
            });
        }
        for (boolean result : results) {
            if (!result) {
                return false;
            }
        }
        return true;
    }
}

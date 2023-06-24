package org.popcraft.bolt.util;

import org.popcraft.bolt.access.Access;

public record ProtectableConfig(Access defaultAccess, boolean lockPermission, boolean autoProtectPermission) {
}

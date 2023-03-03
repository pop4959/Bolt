package org.popcraft.bolt.data;

import java.util.UUID;

public record Profile(UUID uuid, String name) {
    public boolean complete() {
        return uuid != null && name != null;
    }

    public boolean empty() {
        return uuid == null && name == null;
    }
}

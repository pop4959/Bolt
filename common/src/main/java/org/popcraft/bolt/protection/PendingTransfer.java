package org.popcraft.bolt.protection;

import java.util.UUID;

public record PendingTransfer(UUID protection, UUID giver, UUID recipient, long created) {
}

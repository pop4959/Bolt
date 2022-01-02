package org.popcraft.bolt.data.store.migration.lwc;

public record History(int id, int protectionId, String player, int x, int y, int z, int type, int status,
                      String metadata, long timestamp) {
}

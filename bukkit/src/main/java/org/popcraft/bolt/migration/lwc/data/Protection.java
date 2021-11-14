package org.popcraft.bolt.migration.lwc.data;

public record Protection(int id, String owner, int type, int x, int y, int z, String data, int blockId, String world, String password, String date, long lastAccessed) {
}

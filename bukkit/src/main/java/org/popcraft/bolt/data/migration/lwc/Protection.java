package org.popcraft.bolt.data.migration.lwc;

import java.util.Date;

public record Protection(int id, String owner, int type, int x, int y, int z, String data, int blockId, String world,
                         String password, Date date, long lastAccessed) {
}

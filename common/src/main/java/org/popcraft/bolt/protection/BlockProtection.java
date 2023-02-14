package org.popcraft.bolt.protection;

import java.util.Map;
import java.util.UUID;

public class BlockProtection extends Protection {
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String block;

    public BlockProtection(UUID id, UUID owner, String type, long created, long accessed, Map<String, String> access, String world, int x, int y, int z, String block) {
        super(id, owner, type, created, accessed, access);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getBlock() {
        return block;
    }

    @Override
    public String toString() {
        return "BlockProtection{" +
                "id=" + id +
                ", owner=" + owner +
                ", type='" + type + '\'' +
                ", created=" + created +
                ", accessed=" + accessed +
                ", access=" + access +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", block='" + block + '\'' +
                '}';
    }
}

package org.popcraft.bolt.protection;

import org.popcraft.bolt.util.Source;

import java.util.Map;
import java.util.UUID;

public class BlockProtection extends Protection {
    private final String block;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public BlockProtection(UUID id, UUID owner, UUID parent, String type, Map<Source, String> access, String block, String world, int x, int y, int z) {
        super(id, owner, parent, type, access);
        this.block = block;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getBlock() {
        return block;
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

    @Override
    public String toString() {
        return "BlockProtection{" +
                "id=" + id +
                ", owner=" + owner +
                ", parent=" + parent +
                ", type='" + type + '\'' +
                ", access=" + access +
                ", block='" + block + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}

package org.popcraft.bolt.protection;

import org.popcraft.bolt.util.Source;

import java.util.Map;
import java.util.UUID;

public class BlockProtection extends Protection {
    private final String block;
    private String world;
    private int x;
    private int y;
    private int z;

    public BlockProtection(UUID id, UUID owner, String type, Map<Source, String> accessList, String block, String world, int x, int y, int z) {
        super(id, owner, type, accessList);
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

    public void setWorld(String world) {
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return "BlockProtection{" +
                "id=" + id +
                ", accessList=" + accessList +
                ", owner=" + owner +
                ", type='" + type + '\'' +
                ", block='" + block + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}

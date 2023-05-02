package org.popcraft.bolt.protection;

import java.util.Map;
import java.util.UUID;

public class BlockProtection extends Protection {
    private String world;
    private int x;
    private int y;
    private int z;
    private String block;

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

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
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

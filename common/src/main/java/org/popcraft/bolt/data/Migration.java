package org.popcraft.bolt.data;

public interface Migration {
    MemoryStore convert();
}

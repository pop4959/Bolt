package org.popcraft.bolt.data.migration;

import org.popcraft.bolt.data.MemoryStore;

public interface Migration {
    MemoryStore convert();
}

package org.popcraft.bolt.migration;

import org.popcraft.bolt.data.store.MemoryStore;

public interface Migration {
    MemoryStore migrate();
}

package org.popcraft.bolt.data.store.migration;

import org.popcraft.bolt.data.store.MemoryStore;

public interface Migration {
    MemoryStore convert();
}

package org.popcraft.bolt.store.migration;

import org.popcraft.bolt.store.MemoryStore;

public interface Migration {
    MemoryStore convert();
}

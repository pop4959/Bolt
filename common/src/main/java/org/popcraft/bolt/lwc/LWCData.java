package org.popcraft.bolt.lwc;

import java.util.ArrayList;
import java.util.List;

public class LWCData {
    private final List<Block> blocks = new ArrayList<>();
    private final List<Default> defaults = new ArrayList<>();
    private final List<History> history = new ArrayList<>();
    private final List<Internal> internal = new ArrayList<>();
    private final List<Protection> protections = new ArrayList<>();

    public List<Block> getBlocks() {
        return blocks;
    }

    public List<Default> getDefaults() {
        return defaults;
    }

    public List<History> getHistory() {
        return history;
    }

    public List<Internal> getInternal() {
        return internal;
    }

    public List<Protection> getProtections() {
        return protections;
    }
}

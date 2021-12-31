package org.popcraft.bolt.command;

import org.popcraft.bolt.Bolt;

import java.util.List;

public abstract class BoltCommand<T> {
    protected Bolt bolt;

    protected BoltCommand(Bolt bolt) {
        this.bolt = bolt;
    }

    public abstract void execute(T sender, Arguments arguments);

    public abstract List<String> suggestions();
}

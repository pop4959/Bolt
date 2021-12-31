package org.popcraft.bolt.command;

import org.bukkit.command.CommandSender;
import org.popcraft.bolt.Bolt;

import java.util.Collections;
import java.util.List;

public abstract class BukkitCommand extends BoltCommand<CommandSender> {
    protected BukkitCommand(Bolt bolt) {
        super(bolt);
    }

    @Override
    public List<String> suggestions() {
        return Collections.emptyList();
    }
}

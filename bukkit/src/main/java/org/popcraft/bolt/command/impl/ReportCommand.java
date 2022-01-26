package org.popcraft.bolt.command.impl;

import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Metrics;

import java.util.Collections;
import java.util.List;

public class ReportCommand extends BoltCommand {
    public ReportCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, "Hits: %d".formatted(Metrics.getProtectionHits()));
        BoltComponents.sendMessage(sender, "Misses: %d".formatted(Metrics.getProtectionHits()));
        Metrics.getProtectionAccesses().forEach((name, count) -> BoltComponents.sendMessage(sender, "%s: %d".formatted(name, count)));
    }

    @Override
    public List<String> suggestions() {
        return Collections.emptyList();
    }
}

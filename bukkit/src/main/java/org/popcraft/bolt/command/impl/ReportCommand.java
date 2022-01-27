package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportCommand extends BoltCommand {
    private long previousHits, previousMisses;
    private Map<Metrics.ProtectionAccess, Long> previousExecutionCounts;

    public ReportCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        final long hits = Metrics.getProtectionHits();
        final long misses = Metrics.getProtectionMisses();
        final boolean hitsChanged = hits != previousHits;
        final boolean missesChanged = misses != previousMisses;
        BoltComponents.sendMessage(sender, "Hits: <hits>", Template.of("hits", Component.empty().append(Component.text(hitsChanged ? "%d -> ".formatted(previousHits) : "")).append(Component.text(hits).color(hitsChanged ? NamedTextColor.RED : NamedTextColor.WHITE))));
        BoltComponents.sendMessage(sender, "Misses: <misses>", Template.of("misses", Component.empty().append(Component.text(missesChanged ? "%d -> ".formatted(previousMisses) : "")).append(Component.text(misses).color(missesChanged ? NamedTextColor.RED : NamedTextColor.WHITE))));
        final Map<Metrics.ProtectionAccess, Long> protectionAccessCounts = Metrics.getProtectionAccessCounts();
        final Map<String, List<Metrics.ProtectionAccess>> protectionAccessListByType = protectionAccessCounts.keySet().stream().collect(Collectors.groupingBy(Metrics.ProtectionAccess::type, Collectors.toList()));
        protectionAccessListByType.forEach((type, list) -> {
            BoltComponents.sendMessage(sender, "<type>", Template.of("type", type));
            list.forEach(protectionAccess -> {
                final long count = protectionAccessCounts.get(protectionAccess);
                final long previousCount = previousExecutionCounts.getOrDefault(protectionAccess, 0L);
                final boolean countChanged = previousExecutionCounts != null && count != previousCount;
                BoltComponents.sendMessage(sender, "<consumer>: <count>", Template.of("consumer", protectionAccess.consumer()), Template.of("count", Component.empty().append(Component.text(countChanged ? "%d -> ".formatted(previousCount) : "")).append(Component.text(count).color(countChanged ? NamedTextColor.RED : NamedTextColor.WHITE))));
            });
        });
        this.previousHits = hits;
        this.previousMisses = misses;
        this.previousExecutionCounts = new HashMap<>(protectionAccessCounts);
    }

    @Override
    public List<String> suggestions() {
        return Collections.emptyList();
    }
}

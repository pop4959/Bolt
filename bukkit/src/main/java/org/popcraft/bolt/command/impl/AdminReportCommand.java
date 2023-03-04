package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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

public class AdminReportCommand extends BoltCommand {
    private final Map<Metrics.ProtectionAccess, Long> previousExecutionCounts = new HashMap<>();
    private long previousHits;
    private long previousMisses;

    public AdminReportCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!Metrics.isEnabled()) {
            Metrics.setEnabled(true);
            BoltComponents.sendMessage(sender, "Enabling reports. This will have a performance impact. Type '/bolt report disable' to turn off reports. Type '/bolt report' again to generate an updated report.");
            return;
        }
        final boolean disable = arguments.next() != null;
        if (disable) {
            Metrics.setEnabled(false);
            previousHits = 0;
            previousMisses = 0;
            previousExecutionCounts.clear();
            BoltComponents.sendMessage(sender, "Disabling reports.");
            return;
        }
        final long hits = Metrics.getProtectionHits();
        final long misses = Metrics.getProtectionMisses();
        final boolean hitsChanged = hits != previousHits;
        final boolean missesChanged = misses != previousMisses;
        BoltComponents.sendMessage(sender, "Hits: <hits>", Placeholder.component("hits", changeComponent(hitsChanged, previousHits, hits)));
        BoltComponents.sendMessage(sender, "Misses: <misses>", Placeholder.component("misses", changeComponent(missesChanged, previousMisses, misses)));
        final Map<Metrics.ProtectionAccess, Long> protectionAccessCounts = Metrics.getProtectionAccessCounts();
        final Map<String, List<Metrics.ProtectionAccess>> protectionAccessListByType = protectionAccessCounts.keySet().stream().collect(Collectors.groupingBy(Metrics.ProtectionAccess::type, Collectors.toList()));
        protectionAccessListByType.forEach((type, list) -> {
            BoltComponents.sendMessage(sender, "<type>", Placeholder.unparsed("type", type));
            list.forEach(protectionAccess -> {
                final long count = protectionAccessCounts.get(protectionAccess);
                final long previousCount = previousExecutionCounts.getOrDefault(protectionAccess, 0L);
                final boolean countChanged = count != previousCount;
                BoltComponents.sendMessage(sender, "<consumer>: <count>", Placeholder.unparsed("consumer", protectionAccess.consumer()), Placeholder.component("count", changeComponent(countChanged, previousCount, count)));
            });
        });
        this.previousHits = hits;
        this.previousMisses = misses;
        this.previousExecutionCounts.clear();
        this.previousExecutionCounts.putAll(protectionAccessCounts);
    }

    private Component changeComponent(final boolean changed, final long before, final long after) {
        return Component.empty().append(Component.text(changed ? "%d -> ".formatted(before) : "")).append(Component.text(after).color(changed ? NamedTextColor.RED : NamedTextColor.WHITE));
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        return Collections.emptyList();
    }
}

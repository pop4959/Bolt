package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Profile;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminFindCommand extends BoltCommand {
    private static final int RESULTS_PER_PAGE = 4;

    public AdminFindCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() < 1) {
            shortHelp(sender, arguments);
            return;
        }
        final String player = arguments.next();
        final Integer page = arguments.nextAsInteger();
        Profiles.findOrLookupProfileByName(player).thenAccept(playerProfile -> SchedulerUtil.schedule(plugin, sender, () -> {
            if (!playerProfile.complete()) {
                BoltComponents.sendMessage(
                        sender,
                        Translation.PLAYER_NOT_FOUND,
                        Placeholder.component(Translation.Placeholder.PLAYER, Component.text(player))
                );
                return;
            }
            final Store store = plugin.getBolt().getStore();
            final List<BlockProtection> blockProtectionsFromPlayer = store.loadBlockProtections().join().stream()
                    .filter(blockProtection -> playerProfile.uuid().equals(blockProtection.getOwner()))
                    .sorted(Comparator.comparingLong(BlockProtection::getCreated).reversed())
                    .toList();
            runPage(sender, playerProfile, blockProtectionsFromPlayer, page == null ? 0 : page);
        }));
    }

    private void runPage(final CommandSender sender, final Profile playerProfile, final List<BlockProtection> blockProtectionsFromPlayer, final int page) {
        final int skip = RESULTS_PER_PAGE * page;
        BoltComponents.sendMessage(sender, Translation.FIND_HEADER);
        final long now = System.currentTimeMillis();
        final AtomicInteger displayed = new AtomicInteger();
        blockProtectionsFromPlayer.stream().skip(skip).limit(RESULTS_PER_PAGE).forEach(blockProtection -> {
            final long elapsed = now - blockProtection.getCreated();
            final Duration duration = Duration.of(elapsed, ChronoUnit.MILLIS);
            final String time = "%d:%02d".formatted(duration.toHours(), duration.toMinutesPart());
            BoltComponents.sendMessage(
                    sender,
                    Translation.FIND_RESULT,
                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(blockProtection, sender)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(blockProtection, sender)),
                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(playerProfile.name())),
                    Placeholder.component(Translation.Placeholder.TIME, Component.text(time)),
                    Placeholder.component(Translation.Placeholder.WORLD, Component.text(blockProtection.getWorld())),
                    Placeholder.component(Translation.Placeholder.X, Component.text(blockProtection.getX())),
                    Placeholder.component(Translation.Placeholder.Y, Component.text(blockProtection.getY())),
                    Placeholder.component(Translation.Placeholder.Z, Component.text(blockProtection.getZ()))
            );
            displayed.incrementAndGet();
        });
        final int numberDisplayed = displayed.get();
        if (numberDisplayed == 0) {
            BoltComponents.sendMessage(sender, Translation.FIND_NONE);
        } else if (numberDisplayed == RESULTS_PER_PAGE) {
            BoltComponents.sendClickableMessage(
                    sender,
                    Translation.FIND_NEXT,
                    ClickEvent.runCommand("/bolt admin find %s %s".formatted(playerProfile.name(), page + 1))
            );
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_ADMIN_FIND,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin find"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_FIND);
    }
}

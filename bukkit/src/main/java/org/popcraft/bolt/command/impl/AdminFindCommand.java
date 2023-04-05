package org.popcraft.bolt.command.impl;

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
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Protections;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.popcraft.bolt.lang.Translator.translate;

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
        final Profile playerProfile = BukkitAdapter.findOrLookupProfileByName(player).join();
        if (!playerProfile.complete()) {
            BoltComponents.sendMessage(
                    sender,
                    Translation.PLAYER_NOT_FOUND,
                    Placeholder.unparsed(Translation.Placeholder.PLAYER, player)
            );
            return;
        }
        final Store store = plugin.getBolt().getStore();
        final List<BlockProtection> blockProtectionsFromPlayer = store.loadBlockProtections().join().stream()
                .filter(blockProtection -> playerProfile.uuid().equals(blockProtection.getOwner()))
                .sorted(Comparator.comparingLong(BlockProtection::getCreated).reversed())
                .toList();
        runPage(sender, playerProfile, blockProtectionsFromPlayer);
    }

    private void runPage(final CommandSender sender, final Profile playerProfile, final List<BlockProtection> blockProtectionsFromPlayer) {
        BoltComponents.sendMessage(sender, Translation.FIND_HEADER);
        final long now = System.currentTimeMillis();
        blockProtectionsFromPlayer.stream().limit(RESULTS_PER_PAGE).forEach(blockProtection -> {
            final long elapsed = now - blockProtection.getCreated();
            final Duration duration = Duration.of(elapsed, ChronoUnit.MILLIS);
            final String time = "%d:%02d".formatted(duration.toHours(), duration.toMinutesPart());
            BoltComponents.sendMessage(
                    sender,
                    Translation.FIND_RESULT,
                    Placeholder.unparsed(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(blockProtection)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(blockProtection)),
                    Placeholder.unparsed(Translation.Placeholder.PLAYER, playerProfile.name()),
                    Placeholder.unparsed(Translation.Placeholder.TIME, time),
                    Placeholder.unparsed(Translation.Placeholder.WORLD, blockProtection.getWorld()),
                    Placeholder.unparsed(Translation.Placeholder.X, String.valueOf(blockProtection.getX())),
                    Placeholder.unparsed(Translation.Placeholder.Y, String.valueOf(blockProtection.getY())),
                    Placeholder.unparsed(Translation.Placeholder.Z, String.valueOf(blockProtection.getZ()))
            );
        });
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
                Placeholder.unparsed(Translation.Placeholder.COMMAND, "/bolt admin find")
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_FIND);
    }
}

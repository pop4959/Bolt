package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Profile;
import org.popcraft.bolt.data.Store;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AdminTransferCommand extends BoltCommand {
    public AdminTransferCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        if (arguments.remaining() < 1) {
            shortHelp(sender, arguments);
            return;
        }
        final String owner = arguments.next();
        final String newOwner = arguments.next();
        final CompletableFuture<Profile> ownerProfileFuture = BukkitAdapter.findOrLookupProfileByName(owner);
        final CompletableFuture<Profile> newOwnerProfileFuture = BukkitAdapter.findOrLookupProfileByName(newOwner);
        if (newOwner != null) {
            CompletableFuture.allOf(ownerProfileFuture, newOwnerProfileFuture).thenRun(() -> {
                final Profile ownerProfile = ownerProfileFuture.join();
                final Profile newOwnerProfile = newOwnerProfileFuture.join();
                if (ownerProfile.uuid() == null) {
                    SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                            player,
                            Translation.PLAYER_NOT_FOUND,
                            Placeholder.component(Translation.Placeholder.PLAYER, Component.text(owner))
                    ));
                } else if (newOwnerProfile.uuid() == null) {
                    SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                            player,
                            Translation.PLAYER_NOT_FOUND,
                            Placeholder.component(Translation.Placeholder.PLAYER, Component.text(newOwner))
                    ));
                } else {
                    final Store store = plugin.getBolt().getStore();
                    store.loadBlockProtections().join().stream()
                            .filter(protection -> protection.getOwner().equals(ownerProfile.uuid()))
                            .forEach(protection -> {
                                protection.setOwner(newOwnerProfile.uuid());
                                store.saveBlockProtection(protection);
                            });
                    store.loadEntityProtections().join().stream()
                            .filter(protection -> protection.getOwner().equals(ownerProfile.uuid()))
                            .forEach(protection -> {
                                protection.setOwner(newOwnerProfile.uuid());
                                store.saveEntityProtection(protection);
                            });
                    SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_TRANSFER_ALL,
                            Placeholder.component(Translation.Placeholder.OLD_PLAYER, Component.text(owner)),
                            Placeholder.component(Translation.Placeholder.NEW_PLAYER, Component.text(newOwner))
                    ));
                }
            });
        } else {
            ownerProfileFuture.thenAccept(profile -> {
                if (profile.uuid() != null) {
                    plugin.player(player).setAction(new Action(Action.Type.TRANSFER, profile.uuid().toString(), true));
                    BoltComponents.sendMessage(player, Translation.CLICK_TRANSFER);
                } else {
                    BoltComponents.sendMessage(
                            player,
                            Translation.PLAYER_NOT_FOUND,
                            Placeholder.component(Translation.Placeholder.PLAYER, Component.text(owner))
                    );
                }
            });
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
                Translation.HELP_COMMAND_SHORT_ADMIN_TRANSFER,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt admin transfer"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_ADMIN_TRANSFER);
    }
}

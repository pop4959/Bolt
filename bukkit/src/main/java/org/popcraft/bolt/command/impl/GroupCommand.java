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
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GroupCommand extends BoltCommand {
    public GroupCommand(BoltPlugin plugin) {
        super(plugin);
    }

    @Override
    public void execute(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof final Player player)) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            return;
        }
        if (arguments.remaining() < 2) {
            BoltComponents.sendMessage(sender, Translation.COMMAND_NOT_ENOUGH_ARGS);
            return;
        }
        final String action = arguments.next().toLowerCase();
        final String group = arguments.next();
        final List<CompletableFuture<Profile>> memberFutures = new ArrayList<>();
        final List<String> memberNames = new ArrayList<>();
        String member;
        while ((member = arguments.next()) != null) {
            memberFutures.add(BukkitAdapter.findOrLookupProfileByName(member));
            memberNames.add(member);
        }
        CompletableFuture.allOf(memberFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            final List<UUID> members = new ArrayList<>();
            memberFutures.forEach(memberFuture -> {
                if (memberFuture != null) {
                    final Profile memberProfile = memberFuture.join();
                    if (memberProfile.uuid() != null) {
                        members.add(memberProfile.uuid());
                    }
                }
            });
            final Store store = plugin.getBolt().getStore();
            final Group existingGroup = store.loadGroup(group).join();
            switch (action) {
                case "create":
                    if (existingGroup != null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_ALREADY_EXISTS, Placeholder.unparsed("group", group));
                    } else {
                        store.saveGroup(new Group(group, player.getUniqueId(), members));
                        BoltComponents.sendMessage(player, Translation.GROUP_CREATED, Placeholder.unparsed("group", group));
                    }
                    break;
                case "delete":
                    if (existingGroup == null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_DOESNT_EXIST, Placeholder.unparsed("group", group));
                    } else if (!existingGroup.getOwner().equals(player.getUniqueId())) {
                        BoltComponents.sendMessage(player, Translation.GROUP_NOT_OWNER, Placeholder.unparsed("group", group));
                    } else {
                        store.removeGroup(existingGroup);
                        BoltComponents.sendMessage(player, Translation.GROUP_DELETED, Placeholder.unparsed("group", group));
                    }
                    break;

                case "add":
                    if (existingGroup == null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_DOESNT_EXIST, Placeholder.unparsed("group", group));
                    } else if (!existingGroup.getOwner().equals(player.getUniqueId())) {
                        BoltComponents.sendMessage(player, Translation.GROUP_NOT_OWNER, Placeholder.unparsed("group", group));
                    } else {
                        existingGroup.getMembers().addAll(members);
                        store.saveGroup(existingGroup);
                        memberNames.forEach(memberName -> BoltComponents.sendMessage(player, Translation.GROUP_PLAYER_ADD, Placeholder.unparsed("player", memberName), Placeholder.unparsed("group", group)));
                    }
                    break;
                case "remove":
                    if (existingGroup == null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_DOESNT_EXIST, Placeholder.unparsed("group", group));
                    } else if (!existingGroup.getOwner().equals(player.getUniqueId())) {
                        BoltComponents.sendMessage(player, Translation.GROUP_NOT_OWNER, Placeholder.unparsed("group", group));
                    } else {
                        existingGroup.getMembers().removeAll(members);
                        store.saveGroup(existingGroup);
                        memberNames.forEach(memberName -> BoltComponents.sendMessage(player, Translation.GROUP_PLAYER_REMOVE, Placeholder.unparsed("player", memberName), Placeholder.unparsed("group", group)));
                    }
                    break;
                case "list":
                    if (existingGroup == null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_DOESNT_EXIST, Placeholder.unparsed("group", group));
                    } else {
                        final List<CompletableFuture<Profile>> existingMemberFutures = new ArrayList<>();
                        existingGroup.getMembers().forEach(existingMember -> existingMemberFutures.add(BukkitAdapter.findOrLookupProfileByUniqueId(existingMember)));
                        CompletableFuture.allOf(existingMemberFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                            final List<String> existingMemberNames = new ArrayList<>();
                            existingMemberFutures.forEach(existingMemberFuture -> {
                                if (existingMemberFuture != null) {
                                    final Profile existingMemberProfile = existingMemberFuture.join();
                                    if (existingMemberProfile.name() != null) {
                                        existingMemberNames.add(existingMemberProfile.name());
                                    }
                                }
                            });
                            final String memberList = String.join(", ", existingMemberNames);
                            BoltComponents.sendMessage(player, Translation.GROUP_LIST_MEMBERS, Placeholder.unparsed("group", group), Placeholder.unparsed("members", memberList));
                        });
                    }
                    break;
            }
        });
    }

    @Override
    public List<String> suggestions(Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("create", "delete", "add", "remove", "list");
        }
        arguments.next();
        final Set<String> alreadyAdded = new HashSet<>();
        String added;
        while ((added = arguments.next()) != null) {
            alreadyAdded.add(added);
        }
        return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> !alreadyAdded.contains(name)).toList();
    }
}

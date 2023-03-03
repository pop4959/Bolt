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
        final List<String> members = new ArrayList<>();
        String member;
        while ((member = arguments.next()) != null) {
            members.add(member);
        }
        BukkitAdapter.findOrLookupProfilesByNames(members).thenAccept(profiles -> {
            final Store store = plugin.getBolt().getStore();
            final Group existingGroup = store.loadGroup(group).join();
            final List<Profile> completeProfiles = profiles.stream().filter(Profile::complete).toList();
            final List<UUID> uuids = new ArrayList<>(completeProfiles.stream().map(Profile::uuid).toList());
            final List<String> names = new ArrayList<>(completeProfiles.stream().map(Profile::name).toList());
            switch (action) {
                case "create":
                    if (existingGroup != null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_ALREADY_EXISTS, Placeholder.unparsed("group", group));
                    } else {
                        store.saveGroup(new Group(group, player.getUniqueId(), uuids));
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
                        existingGroup.getMembers().addAll(uuids);
                        store.saveGroup(existingGroup);
                        names.forEach(name -> BoltComponents.sendMessage(player, Translation.GROUP_PLAYER_ADD, Placeholder.unparsed("player", name), Placeholder.unparsed("group", group)));
                    }
                    break;
                case "remove":
                    if (existingGroup == null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_DOESNT_EXIST, Placeholder.unparsed("group", group));
                    } else if (!existingGroup.getOwner().equals(player.getUniqueId())) {
                        BoltComponents.sendMessage(player, Translation.GROUP_NOT_OWNER, Placeholder.unparsed("group", group));
                    } else {
                        existingGroup.getMembers().removeAll(uuids);
                        store.saveGroup(existingGroup);
                        names.forEach(name -> BoltComponents.sendMessage(player, Translation.GROUP_PLAYER_REMOVE, Placeholder.unparsed("player", name), Placeholder.unparsed("group", group)));
                    }
                    break;
                case "list":
                    if (existingGroup == null) {
                        BoltComponents.sendMessage(player, Translation.GROUP_DOESNT_EXIST, Placeholder.unparsed("group", group));
                    } else {
                        BukkitAdapter.findOrLookupProfilesByUniqueIds(existingGroup.getMembers()).thenAccept(existingProfiles -> {
                            final List<String> memberNames = existingProfiles.stream().filter(Profile::complete).map(Profile::name).toList();
                            final String memberList = String.join(", ", memberNames);
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

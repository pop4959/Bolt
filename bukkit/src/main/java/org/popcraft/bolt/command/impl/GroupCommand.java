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
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Group;
import org.popcraft.bolt.util.SchedulerUtil;

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
            shortHelp(sender, arguments);
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
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_ALREADY_EXISTS,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else {
                        store.saveGroup(new Group(group, player.getUniqueId(), uuids));
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_CREATED,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    }
                    break;
                case "delete":
                    if (existingGroup == null) {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_DOESNT_EXIST,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else if (!existingGroup.getOwner().equals(player.getUniqueId())) {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_NOT_OWNER,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else {
                        store.removeGroup(existingGroup);
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_DELETED,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    }
                    break;
                case "add":
                    if (existingGroup == null) {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_DOESNT_EXIST,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else if (!existingGroup.getOwner().equals(player.getUniqueId())) {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_NOT_OWNER,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else {
                        existingGroup.getMembers().addAll(uuids);
                        store.saveGroup(existingGroup);
                        names.forEach(name -> SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_PLAYER_ADD,
                                Placeholder.component(Translation.Placeholder.PLAYER, Component.text(name)),
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        )));
                    }
                    break;
                case "remove":
                    if (existingGroup == null) {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_DOESNT_EXIST,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else if (!existingGroup.getOwner().equals(player.getUniqueId())) {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_NOT_OWNER,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else {
                        existingGroup.getMembers().removeAll(uuids);
                        store.saveGroup(existingGroup);
                        names.forEach(name -> SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_PLAYER_REMOVE,
                                Placeholder.component(Translation.Placeholder.PLAYER, Component.text(name)),
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        )));
                    }
                    break;
                case "list":
                    if (existingGroup == null) {
                        SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                player,
                                Translation.GROUP_DOESNT_EXIST,
                                Placeholder.component(Translation.Placeholder.GROUP, Component.text(group))
                        ));
                    } else {
                        BukkitAdapter.findOrLookupProfilesByUniqueIds(existingGroup.getMembers()).thenAccept(existingProfiles -> {
                            final List<String> memberNames = existingProfiles.stream().filter(Profile::complete).map(Profile::name).toList();
                            final String memberList = String.join(", ", memberNames);
                            SchedulerUtil.schedule(plugin, player, () -> BoltComponents.sendMessage(
                                    player,
                                    Translation.GROUP_LIST_MEMBERS,
                                    Placeholder.component(Translation.Placeholder.GROUP, Component.text(group)),
                                    Placeholder.component(Translation.Placeholder.GROUP_MEMBERS, Component.text(memberList))
                            ));
                        });
                    }
                    break;
            }
        });
    }

    @Override
    public List<String> suggestions(CommandSender sender, Arguments arguments) {
        if (arguments.remaining() == 0) {
            return Collections.emptyList();
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            return List.of("create", "delete", "add", "remove", "list");
        }
        arguments.next();
        if (arguments.remaining() == 0) {
            if (sender instanceof final Player player) {
                return plugin.getPlayersOwnedGroups(player);
            } else {
                return Collections.emptyList();
            }
        }
        final Set<String> alreadyAdded = new HashSet<>();
        String added;
        while ((added = arguments.next()) != null) {
            alreadyAdded.add(added);
        }
        return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> !alreadyAdded.contains(name)).toList();
    }

    @Override
    public void shortHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(
                sender,
                Translation.HELP_COMMAND_SHORT_GROUP,
                Placeholder.component(Translation.Placeholder.COMMAND, Component.text("/bolt group")),
                Placeholder.component(Translation.Placeholder.LITERAL, Component.text("(create|delete|add|remove|list)"))
        );
    }

    @Override
    public void longHelp(CommandSender sender, Arguments arguments) {
        BoltComponents.sendMessage(sender, Translation.HELP_COMMAND_LONG_GROUP);
    }
}

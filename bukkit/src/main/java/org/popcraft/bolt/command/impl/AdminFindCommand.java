package org.popcraft.bolt.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.command.Arguments;
import org.popcraft.bolt.command.BoltCommand;
import org.popcraft.bolt.data.Profile;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.lang.Translator;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.PaperUtil;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;
import org.popcraft.bolt.util.Time;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.popcraft.bolt.util.BoltComponents.getLocaleOf;
import static org.popcraft.bolt.util.BoltComponents.resolveTranslation;

public class AdminFindCommand extends BoltCommand {
    private static final int RESULTS_PER_PAGE = 4;
    private static final int PAGES_BEFORE_AND_AFTER = 4;

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
            final List<Protection> protectionsFromPlayer = plugin.loadProtections().stream()
                    .filter(protection -> playerProfile.uuid().equals(protection.getOwner()))
                    .sorted(Comparator.comparingLong(Protection::getCreated).reversed())
                    .toList();
            runPage(sender, playerProfile, protectionsFromPlayer, page == null ? 0 : page);
        }));
    }

    private void runPage(final CommandSender sender, final Profile playerProfile, final List<Protection> protectionsFromPlayer, final int page) {
        final int skip = RESULTS_PER_PAGE * page;
        final boolean newFindFormat = Translator.isTranslated(Translation.FIND_HEADER_NEW, getLocaleOf(sender));
        final int total = protectionsFromPlayer.size();
        final int totalPages = Math.max(0, (int) Math.ceil((double) total / RESULTS_PER_PAGE) - 1);
        if (newFindFormat) {
            BoltComponents.sendMessage(
                    sender,
                    Translation.FIND_HEADER_NEW,
                    Placeholder.component(Translation.Placeholder.FIRST, Component.text(RESULTS_PER_PAGE * page + 1)),
                    Placeholder.component(Translation.Placeholder.LAST, Component.text(Math.min(RESULTS_PER_PAGE * (page + 1), total))),
                    Placeholder.component(Translation.Placeholder.COUNT, Component.text(total))
            );
        } else {
            BoltComponents.sendMessage(sender, Translation.FIND_HEADER);
        }
        final AtomicInteger displayed = new AtomicInteger();
        protectionsFromPlayer.stream().skip(skip).limit(RESULTS_PER_PAGE).forEach(protection -> {
            final Location location = switch (protection) {
                case BlockProtection block -> new Location(plugin.getServer().getWorld(block.getWorld()), block.getX() + 0.5, block.getY(), block.getZ() + 0.5);
                case EntityProtection entityProtection -> {
                    final Entity entity = plugin.getServer().getEntity(entityProtection.getId());
                    yield entity == null ? null : entity.getLocation();
                }
            };

            if (location == null) {
                BoltComponents.sendMessage(
                    sender,
                    Translation.FIND_RESULT_UNKNOWN,
                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, sender)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, sender)),
                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(playerProfile.name())),
                    Placeholder.component(Translation.Placeholder.TIME, Time.relativeTimestamp(protection.getCreated(), sender, 1))
                );
            } else {
                final ClickEvent teleport = plugin.getCallbackManager().registerPlayerOnly(player -> PaperUtil.teleportAsync(player, location));
                BoltComponents.sendMessage(
                    sender,
                    Translation.FIND_RESULT,
                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, sender)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, sender)),
                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(playerProfile.name())),
                    Placeholder.component(Translation.Placeholder.TIME, Time.relativeTimestamp(protection.getCreated(), sender, 1)),
                    Placeholder.component(Translation.Placeholder.WORLD, Component.text(Objects.requireNonNull(location.getWorld()).getName())),
                    Placeholder.component(Translation.Placeholder.X, Component.text(location.getBlockX())),
                    Placeholder.component(Translation.Placeholder.Y, Component.text(location.getBlockY())),
                    Placeholder.component(Translation.Placeholder.Z, Component.text(location.getBlockZ())),
                    Placeholder.styling(Translation.Placeholder.COMMAND, teleport, HoverEvent.showText(resolveTranslation(Translation.FIND_TELEPORT, sender)))
                );
            }
            displayed.incrementAndGet();
        });
        final int numberDisplayed = displayed.get();
        if (numberDisplayed == 0) {
            BoltComponents.sendMessage(sender, Translation.FIND_NONE);
        } else if (newFindFormat) {
            Component pages = Component.empty();
            final int firstPage = Math.max(0, page - PAGES_BEFORE_AND_AFTER);
            final int lastPage = Math.min(page + PAGES_BEFORE_AND_AFTER, totalPages);
            pages = pages.append(resolveTranslation(
                    firstPage == page ? Translation.FIND_NEXT_NEW_PAGE_CURRENT : Translation.FIND_NEXT_NEW_PAGE_OTHER,
                    sender,
                    Placeholder.component(Translation.Placeholder.PAGE, Component.text(firstPage + 1)
                            .clickEvent(ClickEvent.runCommand("/bolt admin find %s %s".formatted(playerProfile.name(), firstPage))))
            ));
            for (int p = firstPage + 1; p <= lastPage; ++p) {
                pages = pages.append(resolveTranslation(Translation.FIND_NEXT_NEW_PAGE_SEPARATOR, sender));
                pages = pages.append(resolveTranslation(
                        p == page ? Translation.FIND_NEXT_NEW_PAGE_CURRENT : Translation.FIND_NEXT_NEW_PAGE_OTHER,
                        sender,
                        Placeholder.component(Translation.Placeholder.PAGE, Component.text(p + 1)
                                .clickEvent(ClickEvent.runCommand("/bolt admin find %s %s".formatted(playerProfile.name(), p))))
                ));
            }
            BoltComponents.sendMessage(
                    sender,
                    Translation.FIND_NEXT_NEW,
                    Placeholder.component(Translation.Placeholder.PAGES, pages),
                    Placeholder.component(Translation.Placeholder.PAGE, resolveTranslation(
                            totalPages == page ? Translation.FIND_NEXT_NEW_PAGE_CURRENT : Translation.FIND_NEXT_NEW_PAGE_OTHER,
                            sender,
                            Placeholder.component(Translation.Placeholder.PAGE, Component.text(totalPages + 1)
                                    .clickEvent(ClickEvent.runCommand("/bolt admin find %s %s".formatted(playerProfile.name(), totalPages))))))
            );
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

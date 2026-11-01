package org.popcraft.bolt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.Profile;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.protection.PendingTransfer;
import org.popcraft.bolt.protection.Protection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.popcraft.bolt.util.BoltComponents.resolveTranslation;

public class TransferRequests {
    private final BoltPlugin plugin;

    public TransferRequests(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(final Protection protection, final UUID recipient) {
        final UUID giver = protection.getOwner();
        plugin.getBolt().getStore().savePendingTransfer(new PendingTransfer(protection.getId(), giver, recipient, System.currentTimeMillis()));
        final Player recipientPlayer = plugin.getServer().getPlayer(recipient);
        if (recipientPlayer == null) {
            return;
        }
        final long count = matching(transfer -> transfer.recipient().equals(recipient) && transfer.giver().equals(giver)).size();
        SchedulerUtil.schedule(plugin, recipientPlayer, () -> {
            final Profile profile = Profiles.findProfileByUniqueId(giver);
            BoltComponents.sendMessage(
                    recipientPlayer,
                    Translation.TRANSFER_REQUEST_RECEIVED,
                    plugin.isUseActionBar(),
                    Placeholder.component(Translation.Placeholder.PLAYER, Optional.ofNullable(profile.name()).<Component>map(Component::text).orElse(resolveTranslation(Translation.UNKNOWN, recipientPlayer))),
                    Placeholder.component(Translation.Placeholder.COUNT, Component.text(count))
            );
        });
    }

    public boolean hasAny(final UUID player) {
        return !matching(transfer -> transfer.giver().equals(player) || transfer.recipient().equals(player)).isEmpty();
    }

    public List<UUID> givers(final UUID recipient) {
        return matching(transfer -> transfer.recipient().equals(recipient)).stream().map(PendingTransfer::giver).distinct().toList();
    }

    public List<UUID> recipients(final UUID giver) {
        return matching(transfer -> transfer.giver().equals(giver)).stream().map(PendingTransfer::recipient).distinct().toList();
    }

    public List<PendingTransfer> incoming(final UUID recipient) {
        return valid(transfer -> transfer.recipient().equals(recipient));
    }

    public List<PendingTransfer> outgoing(final UUID giver) {
        return valid(transfer -> transfer.giver().equals(giver));
    }

    public int accept(final UUID recipient, final UUID giver) {
        final List<PendingTransfer> matching = matching(transfer -> transfer.recipient().equals(recipient) && transfer.giver().equals(giver));
        if (matching.isEmpty()) {
            return 0;
        }
        final Map<UUID, Protection> protections = protectionsById();
        int accepted = 0;
        for (final PendingTransfer transfer : matching) {
            final Protection protection = protections.get(transfer.protection());
            if (isValid(transfer, protection)) {
                protection.setOwner(recipient);
                plugin.saveProtection(protection);
                ++accepted;
            }
            plugin.getBolt().getStore().removePendingTransfer(transfer);
        }
        return accepted;
    }

    public int deny(final UUID recipient, final UUID giver) {
        return discard(transfer -> transfer.recipient().equals(recipient) && transfer.giver().equals(giver));
    }

    public int cancel(final UUID giver, final UUID recipient) {
        return discard(transfer -> transfer.giver().equals(giver) && transfer.recipient().equals(recipient));
    }

    public void clear(final UUID protectionId) {
        for (final PendingTransfer transfer : matching(transfer -> transfer.protection().equals(protectionId))) {
            plugin.getBolt().getStore().removePendingTransfer(transfer);
        }
    }

    public Map<UUID, Protection> protectionsById() {
        final Map<UUID, Protection> protections = new HashMap<>();
        for (final Protection protection : plugin.loadProtections()) {
            protections.put(protection.getId(), protection);
        }
        return protections;
    }

    private List<PendingTransfer> valid(final Predicate<PendingTransfer> filter) {
        final List<PendingTransfer> matching = matching(filter);
        if (matching.isEmpty()) {
            return matching;
        }
        final Map<UUID, Protection> protections = protectionsById();
        final List<PendingTransfer> valid = new ArrayList<>();
        for (final PendingTransfer transfer : matching) {
            if (isValid(transfer, protections.get(transfer.protection()))) {
                valid.add(transfer);
            } else {
                plugin.getBolt().getStore().removePendingTransfer(transfer);
            }
        }
        valid.sort(Comparator.comparingLong(PendingTransfer::created).reversed());
        return valid;
    }

    private int discard(final Predicate<PendingTransfer> filter) {
        final List<PendingTransfer> matching = matching(filter);
        if (matching.isEmpty()) {
            return 0;
        }
        final Map<UUID, Protection> protections = protectionsById();
        int discarded = 0;
        for (final PendingTransfer transfer : matching) {
            if (isValid(transfer, protections.get(transfer.protection()))) {
                ++discarded;
            }
            plugin.getBolt().getStore().removePendingTransfer(transfer);
        }
        return discarded;
    }

    private boolean isValid(final PendingTransfer transfer, final Protection protection) {
        return protection != null && protection.getOwner().equals(transfer.giver());
    }

    private List<PendingTransfer> matching(final Predicate<PendingTransfer> filter) {
        final List<PendingTransfer> matching = new ArrayList<>();
        for (final PendingTransfer transfer : plugin.getBolt().getStore().loadPendingTransfers().join()) {
            if (filter.test(transfer)) {
                matching.add(transfer);
            }
        }
        return matching;
    }
}

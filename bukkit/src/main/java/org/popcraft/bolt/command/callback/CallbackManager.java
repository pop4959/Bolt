package org.popcraft.bolt.command.callback;

import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.SchedulerUtil;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CallbackManager {
    private static final Duration DEFAULT_EXPIRY = Duration.of(1, ChronoUnit.MINUTES);

    private final Map<UUID, Callback> callbacks = new ConcurrentHashMap<>();

    public CallbackManager(BoltPlugin plugin) {
        SchedulerUtil.schedule(plugin, this::cleanupExpired, DEFAULT_EXPIRY.getSeconds() * 20, DEFAULT_EXPIRY.getSeconds() * 20);
    }

    private void cleanupExpired() {
        this.callbacks.entrySet().removeIf(i -> i.getValue().expired());
    }

    public void execute(final CommandSender sender, final UUID id) {
        final Callback callback = this.callbacks.get(id);
        if (callback != null) {
            callback.execute(sender);
        }
    }

    public ClickEvent register(final Consumer<CommandSender> callback) {
        final UUID uuid = UUID.randomUUID();
        this.callbacks.put(uuid, new Callback(Instant.now().plus(DEFAULT_EXPIRY), callback));
        return ClickEvent.runCommand("/bolt callback " + uuid);
    }

    public ClickEvent registerPlayerOnly(final Consumer<Player> callback) {
        return register(sender -> {
            if (sender instanceof Player player) {
                callback.accept(player);
            } else {
                BoltComponents.sendMessage(sender, Translation.COMMAND_PLAYER_ONLY);
            }
        });
    }
}

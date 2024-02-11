package org.popcraft.bolt.command.callback;

import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.util.function.Consumer;

public final class Callback {
    private final Instant expires;
    private final Consumer<CommandSender> callback;

    public Callback(Instant expires, Consumer<CommandSender> callback) {
        this.expires = expires;
        this.callback = callback;
    }

    public boolean expired() {
        return Instant.now().compareTo(expires) > 0;
    }

    public void execute(CommandSender sender) {
        callback.accept(sender);
    }
}

package org.popcraft.bolt.source;

import java.util.concurrent.CompletableFuture;

public class PasswordSourceTransformer implements SourceTransformer {
    @Override
    public CompletableFuture<String> transformIdentifier(String identifier) {
        final Source password = Source.password(identifier);
        return CompletableFuture.completedFuture(password != null ? password.getIdentifier() : null);
    }

    @Override
    public String unTransformIdentifier(String identifier) {
        return "Password";
    }
}

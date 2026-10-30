package org.popcraft.bolt.protection;

import java.util.Objects;

/**
 * Result of a {@link ProtectableTargetResolver}.
 */
public final class ProtectableResult {
    private static final ProtectableResult PASS = new ProtectableResult(null);
    private static final ProtectableResult DENY = new ProtectableResult(null);
    private final ProtectableTarget target;

    private ProtectableResult(final ProtectableTarget target) {
        this.target = target;
    }

    /**
     * The resolver does not recognize the target. The next resolver is consulted, then the config.
     */
    public static ProtectableResult pass() {
        return PASS;
    }

    /**
     * The resolver recognizes the target and it can not be protected. No further resolvers or config are consulted.
     */
    public static ProtectableResult deny() {
        return DENY;
    }

    /**
     * The resolver recognizes the target and it can be protected with the given settings. No further resolvers or
     * config are consulted.
     */
    public static ProtectableResult allow(final ProtectableTarget target) {
        return new ProtectableResult(Objects.requireNonNull(target, "target"));
    }

    public boolean isPass() {
        return this == PASS;
    }

    public boolean isDeny() {
        return this == DENY;
    }

    /**
     * @return the target settings, or {@code null} if this result is not {@link #allow(ProtectableTarget) allow}
     */
    public ProtectableTarget getTarget() {
        return target;
    }
}

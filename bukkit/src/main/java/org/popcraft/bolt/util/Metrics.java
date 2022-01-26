package org.popcraft.bolt.util;

import java.util.HashMap;
import java.util.Map;

public final class Metrics {
    private static final Map<String, Long> protectionAccesses = new HashMap<>();
    private static long protectionHits;
    private static long protectionMisses;

    private Metrics() {
    }

    public static void recordProtectionAccess(boolean hit) {
        if (hit) {
            ++protectionHits;
        } else {
            ++protectionMisses;
        }
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 3) {
            final String method = stackTrace[3].getMethodName();
            protectionAccesses.put(method, protectionAccesses.getOrDefault(method, 0L) + 1);
        }
    }

    public static Map<String, Long> getProtectionAccesses() {
        return protectionAccesses;
    }

    public static long getProtectionHits() {
        return protectionHits;
    }

    public static long getProtectionMisses() {
        return protectionMisses;
    }
}

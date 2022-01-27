package org.popcraft.bolt.util;

import java.util.HashMap;
import java.util.Map;

public final class Metrics {
    private static final String REVERSE_DOMAIN = "org.popcraft";
    private static final String STORE_PACKAGE = REVERSE_DOMAIN + ".bolt.store";
    private static final Map<ProtectionAccess, Long> protectionAccessCounts = new HashMap<>();
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
        for (int i = 0; i < stackTrace.length; ++i) {
            final StackTraceElement stackTraceElement = stackTrace[i];
            if (stackTraceElement.getClassName().startsWith(STORE_PACKAGE)) {
                final String type = stackTraceElement.getMethodName();
                for (int j = i + 1; j < stackTrace.length - 1; ++j) {
                    final StackTraceElement nextStackTraceElement = stackTrace[j + 1];
                    if (!nextStackTraceElement.getClassName().startsWith(REVERSE_DOMAIN)) {
                        final String consumer = stackTrace[j].getMethodName();
                        final ProtectionAccess protectionAccess = new ProtectionAccess(type, consumer);
                        protectionAccessCounts.put(protectionAccess, protectionAccessCounts.getOrDefault(protectionAccess, 0L) + 1);
                        return;
                    }
                }
                break;
            }
        }
    }

    public static Map<ProtectionAccess, Long> getProtectionAccessCounts() {
        return protectionAccessCounts;
    }

    public static long getProtectionHits() {
        return protectionHits;
    }

    public static long getProtectionMisses() {
        return protectionMisses;
    }

    public record ProtectionAccess(String type, String consumer) {
    }
}

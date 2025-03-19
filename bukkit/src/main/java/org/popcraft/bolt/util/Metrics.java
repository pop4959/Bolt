package org.popcraft.bolt.util;

import java.util.HashMap;
import java.util.Map;

public final class Metrics {
    private static final String REVERSE_DOMAIN = "org.popcraft";
    private static final String DATA_PACKAGE = REVERSE_DOMAIN + ".bolt.data";
    private static final Map<ProtectionAccess, Long> protectionAccessCounts = new HashMap<>();
    private static long protectionHits;
    private static long protectionMisses;
    private static boolean enabled;

    private Metrics() {
    }

    public static void recordProtectionAccess(boolean hit) {
        if (!enabled) {
            return;
        }
        if (hit) {
            ++protectionHits;
        } else {
            ++protectionMisses;
        }
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < stackTrace.length; ++i) {
            final StackTraceElement stackTraceElement = stackTrace[i];
            if (stackTraceElement.getClassName().startsWith(DATA_PACKAGE)) {
                final String type = stackTraceElement.getMethodName();
                for (int j = stackTrace.length - 1; j > i; --j) {
                    final StackTraceElement revStackTraceElement = stackTrace[j];
                    if (revStackTraceElement.getClassName().startsWith(REVERSE_DOMAIN)) {
                        final String consumer = revStackTraceElement.getMethodName();
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

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        Metrics.enabled = enabled;
        if (!enabled) {
            protectionAccessCounts.clear();
            protectionHits = 0;
            protectionMisses = 0;
        }
    }

    public record ProtectionAccess(String type, String consumer) {
    }
}

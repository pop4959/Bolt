package org.popcraft.bolt.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Arguments {
    private final Queue<String> remaining = new LinkedList<>();

    public Arguments(String... args) {
        remaining.addAll(Arrays.asList(args));
    }

    public Arguments copy() {
        return new Arguments(new LinkedList<>(remaining).toArray(new String[0]));
    }

    public String next() {
        return remaining.poll();
    }

    public Integer nextAsInteger() {
        final String next = remaining.poll();
        if (next == null) {
            return null;
        }
        try {
            return Integer.parseInt(next);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int remaining() {
        return remaining.size();
    }
}

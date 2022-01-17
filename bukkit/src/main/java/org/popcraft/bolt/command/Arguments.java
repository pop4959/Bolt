package org.popcraft.bolt.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Arguments {
    private final Queue<String> remaining = new LinkedList<>();

    public Arguments(String... args) {
        remaining.addAll(Arrays.asList(args));
    }

    public String next() {
        return remaining.poll();
    }

    public int remaining() {
        return remaining.size();
    }
}

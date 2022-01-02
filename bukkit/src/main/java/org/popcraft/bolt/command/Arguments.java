package org.popcraft.bolt.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Arguments {
    private final Queue<String> arguments = new LinkedList<>();

    public Arguments(String... args) {
        arguments.addAll(Arrays.asList(args));
    }

    public String next() {
        return arguments.poll();
    }
}

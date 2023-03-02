package org.popcraft.bolt.access;

import java.util.Set;

public record Access(String type, Set<String> permissions) {
}

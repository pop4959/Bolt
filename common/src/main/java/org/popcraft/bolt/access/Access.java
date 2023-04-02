package org.popcraft.bolt.access;

import java.util.Set;

public record Access(String type, boolean restricted, Set<String> permissions) {
}

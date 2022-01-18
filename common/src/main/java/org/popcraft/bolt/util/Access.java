package org.popcraft.bolt.util;

import java.util.Set;

public record Access(String type, Set<String> permissions) {
}

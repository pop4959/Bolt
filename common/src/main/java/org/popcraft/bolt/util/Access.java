package org.popcraft.bolt.util;

import java.util.Set;

public record Access(boolean protection, String type, Set<String> permissions) {
}

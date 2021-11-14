package org.popcraft.bolt.data;

import java.util.List;

public record Access(String type, List<String> permissions) {
}

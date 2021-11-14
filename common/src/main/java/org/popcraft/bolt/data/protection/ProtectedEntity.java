package org.popcraft.bolt.data.protection;

import org.popcraft.bolt.data.Source;

import java.util.Map;
import java.util.UUID;

public class ProtectedEntity extends Protection {
    public ProtectedEntity(UUID id, String owner, String type, Map<Source, String> access) {
        super(id, owner, type, access);
    }
}

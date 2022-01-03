package org.popcraft.bolt.data.protection;

import org.popcraft.bolt.data.Source;

import java.util.Map;
import java.util.UUID;

public class EntityProtection extends Protection {
    public EntityProtection(UUID id, UUID owner, String type, Map<Source, String> accessList) {
        super(id, owner, type, accessList);
    }
}

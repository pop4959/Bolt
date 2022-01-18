package org.popcraft.bolt.protection;

import org.popcraft.bolt.util.Source;

import java.util.Map;
import java.util.UUID;

public class EntityProtection extends Protection {
    public EntityProtection(UUID id, UUID owner, String type, Map<Source, String> accessList) {
        super(id, owner, type, accessList);
    }
}

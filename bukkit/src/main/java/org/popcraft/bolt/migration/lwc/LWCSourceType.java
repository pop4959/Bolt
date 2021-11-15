package org.popcraft.bolt.migration.lwc;

import org.popcraft.bolt.data.defaults.DefaultProtectionType;

public enum LWCSourceType {
    PUBLIC(DefaultProtectionType.PUBLIC),
    PASSWORD(DefaultProtectionType.PRIVATE),
    PRIVATE(DefaultProtectionType.PRIVATE),
    UNUSED1(DefaultProtectionType.PRIVATE),
    UNUSED2(DefaultProtectionType.PRIVATE),
    DONATION(DefaultProtectionType.DEPOSIT),
    DISPLAY(DefaultProtectionType.DISPLAY);

    private final DefaultProtectionType migrationType;

    LWCSourceType(DefaultProtectionType migrationType) {
        this.migrationType = migrationType;
    }

    public DefaultProtectionType getMigrationType() {
        return migrationType;
    }
}

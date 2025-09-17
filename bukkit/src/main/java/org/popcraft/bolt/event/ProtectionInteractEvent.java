package org.popcraft.bolt.event;

import net.kyori.adventure.text.Component;
import org.popcraft.bolt.protection.Protection;

public class ProtectionInteractEvent implements Event {
    private final Protection protection;
    private Component protectionName;

    public ProtectionInteractEvent(final Protection protection, final Component protectionName) {
        this.protection = protection;
        this.protectionName = protectionName;
    }

    public Protection getProtection() {
        return protection;
    }

    public Component getProtectionName() {
        return protectionName;
    }

    public void setProtectionName(final Component protectionName) {
        this.protectionName = protectionName;
    }
}

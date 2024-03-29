package org.popcraft.bolt.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class AccessRegistry {
    private final Map<String, Access> protections = new HashMap<>();
    private final Map<String, Access> access = new HashMap<>();

    public void registerProtectionType(final String type, final boolean restricted, final Set<String> permissions) {
        protections.put(type, new Access(type, restricted, permissions));
    }

    public void registerAccessType(final String type, final boolean restricted, final Set<String> permissions) {
        access.put(type, new Access(type, restricted, permissions));
    }

    public void unregisterAll() {
        protections.clear();
        access.clear();
    }

    public Optional<Access> getProtectionByType(String type) {
        return Optional.ofNullable(protections.get(type));
    }

    public Optional<Access> getAccessByType(String type) {
        return Optional.ofNullable(access.get(type));
    }

    public List<String> protectionTypes() {
        return protections.keySet().stream().toList();
    }

    public List<String> accessTypes() {
        return access.keySet().stream().toList();
    }

    public Collection<Access> protections() {
        return new ArrayList<>(protections.values());
    }

    public Collection<Access> access() {
        return new ArrayList<>(access.values());
    }

    public Optional<String> findProtectionTypeWithExactPermissions(final Set<String> permissions) {
        return protections.values().stream()
                .filter(a -> permissions.containsAll(a.permissions()) && a.permissions().containsAll(permissions))
                .map(Access::type)
                .findFirst();
    }

    public Optional<String> findAccessTypeWithExactPermissions(final Set<String> permissions) {
        return access.values().stream()
                .filter(a -> permissions.containsAll(a.permissions()) && a.permissions().containsAll(permissions))
                .map(Access::type)
                .findFirst();
    }
}

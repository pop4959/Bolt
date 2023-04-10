package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.Profile;
import org.popcraft.bolt.data.ProfileCache;
import org.popcraft.bolt.data.SimpleProfileCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Profiles {
    private static final Set<UUID> KNOWN_NULL_LOOKUPS_BY_UUID = ConcurrentHashMap.newKeySet();
    private static final Set<String> KNOWN_NULL_LOOKUPS_BY_NAME = ConcurrentHashMap.newKeySet();
    private static final String NIL_UUID_STRING = "00000000-0000-0000-0000-000000000000";
    public static final UUID NIL_UUID = UUID.fromString(NIL_UUID_STRING);

    private Profiles() {
    }

    public static Profile findProfileByName(final String name) {
        if (name == null || name.isEmpty() || NIL_UUID_STRING.equals(name)) {
            return SimpleProfileCache.EMPTY_PROFILE;
        }
        final ProfileCache profileCache = JavaPlugin.getPlugin(BoltPlugin.class).getProfileCache();
        try {
            return profileCache.getProfile(UUID.fromString(name));
        } catch (final IllegalArgumentException e) {
            final Profile cached = profileCache.getProfile(name);
            if (cached.complete()) {
                return cached;
            }
            final OfflinePlayer offlinePlayer = PaperUtil.getOfflinePlayer(name);
            if (offlinePlayer != null) {
                profileCache.add(offlinePlayer.getUniqueId(), offlinePlayer.getName());
            }
            return profileCache.getProfile(name);
        }
    }

    public static CompletableFuture<Profile> lookupProfileByName(final String name) {
        if (name == null || name.isEmpty() || NIL_UUID_STRING.equals(name) || KNOWN_NULL_LOOKUPS_BY_NAME.contains(name)) {
            return CompletableFuture.completedFuture(SimpleProfileCache.EMPTY_PROFILE);
        }
        final PlayerProfile playerProfile = Bukkit.createPlayerProfile(name);
        final CompletableFuture<PlayerProfile> updatedProfile = playerProfile.update();
        final ProfileCache profileCache = JavaPlugin.getPlugin(BoltPlugin.class).getProfileCache();
        updatedProfile.thenAccept(profile -> {
            if (profile.isComplete()) {
                profileCache.add(profile.getUniqueId(), profile.getName());
            } else {
                KNOWN_NULL_LOOKUPS_BY_NAME.add(name);
            }
        });
        return updatedProfile.thenApply(PlayerProfile::getUniqueId).thenApply(profileCache::getProfile);
    }

    public static CompletableFuture<Profile> findOrLookupProfileByName(final String name) {
        final Profile found = Profiles.findProfileByName(name);
        if (found.complete()) {
            return CompletableFuture.completedFuture(found);
        }
        return lookupProfileByName(name);
    }

    public static CompletableFuture<Collection<Profile>> findOrLookupProfilesByNames(final Collection<String> names) {
        final CompletableFuture<Collection<Profile>> profilesFuture = new CompletableFuture<>();
        final List<CompletableFuture<Profile>> profileFutures = new ArrayList<>();
        names.forEach(name -> profileFutures.add(Profiles.findOrLookupProfileByName(name)));
        CompletableFuture.allOf(profileFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            final List<Profile> profiles = new ArrayList<>();
            profileFutures.forEach(profileFuture -> profiles.add(profileFuture.join()));
            profilesFuture.complete(profiles);
        });
        return profilesFuture;
    }

    public static Profile findProfileByUniqueId(final UUID uuid) {
        if (uuid == null || NIL_UUID.equals(uuid)) {
            return SimpleProfileCache.EMPTY_PROFILE;
        }
        final ProfileCache profileCache = JavaPlugin.getPlugin(BoltPlugin.class).getProfileCache();
        return profileCache.getProfile(uuid);
    }

    public static CompletableFuture<Profile> lookupProfileByUniqueId(final UUID uuid) {
        if (uuid == null || NIL_UUID.equals(uuid) || KNOWN_NULL_LOOKUPS_BY_UUID.contains(uuid)) {
            return CompletableFuture.completedFuture(SimpleProfileCache.EMPTY_PROFILE);
        }
        final PlayerProfile playerProfile = Bukkit.createPlayerProfile(uuid);
        final CompletableFuture<PlayerProfile> updatedProfile = playerProfile.update();
        final ProfileCache profileCache = JavaPlugin.getPlugin(BoltPlugin.class).getProfileCache();
        updatedProfile.thenAccept(profile -> {
            if (profile.isComplete()) {
                profileCache.add(profile.getUniqueId(), profile.getName());
            } else {
                KNOWN_NULL_LOOKUPS_BY_UUID.add(uuid);
            }
        });
        return updatedProfile.thenApply(PlayerProfile::getName).thenApply(profileCache::getProfile);
    }

    public static CompletableFuture<Profile> findOrLookupProfileByUniqueId(final UUID uuid) {
        final Profile found = Profiles.findProfileByUniqueId(uuid);
        if (found.complete()) {
            return CompletableFuture.completedFuture(found);
        }
        return lookupProfileByUniqueId(uuid);
    }

    public static CompletableFuture<Collection<Profile>> findOrLookupProfilesByUniqueIds(final Collection<UUID> uuids) {
        final CompletableFuture<Collection<Profile>> profilesFuture = new CompletableFuture<>();
        final List<CompletableFuture<Profile>> profileFutures = new ArrayList<>();
        uuids.forEach(uuid -> profileFutures.add(Profiles.findOrLookupProfileByUniqueId(uuid)));
        CompletableFuture.allOf(profileFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            final List<Profile> profiles = new ArrayList<>();
            profileFutures.forEach(profileFuture -> profiles.add(profileFuture.join()));
            profilesFuture.complete(profiles);
        });
        return profilesFuture;
    }
}

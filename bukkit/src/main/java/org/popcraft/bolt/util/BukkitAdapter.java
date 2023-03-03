package org.popcraft.bolt.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.Profile;
import org.popcraft.bolt.data.ProfileCache;
import org.popcraft.bolt.data.SimpleProfileCache;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class BukkitAdapter {
    private static final String NIL_UUID_STRING = "00000000-0000-0000-0000-000000000000";
    public static final UUID NIL_UUID = UUID.fromString(NIL_UUID_STRING);

    private BukkitAdapter() {
    }

    public static BlockProtection createBlockProtection(final Block block, final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new BlockProtection(UUID.randomUUID(), owner, type, now, now, new HashMap<>(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), block.getType().name());
    }

    public static BlockLocation blockLocation(final Block block) {
        return new BlockLocation(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public static BlockLocation blockLocation(final BlockState blockState) {
        return new BlockLocation(blockState.getWorld().getName(), blockState.getX(), blockState.getY(), blockState.getZ());
    }

    public static BlockLocation blockLocation(final Location location) {
        Objects.requireNonNull(location.getWorld());
        return new BlockLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockLocation blockLocation(final BlockProtection blockProtection) {
        return new BlockLocation(blockProtection.getWorld(), blockProtection.getX(), blockProtection.getY(), blockProtection.getZ());
    }

    public static EntityProtection createEntityProtection(final Entity entity, final UUID owner, final String type) {
        final long now = System.currentTimeMillis();
        return new EntityProtection(entity.getUniqueId(), owner, type, now, now, new HashMap<>(), entity.getType().name());
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
            if (cached != null) {
                return cached;
            }
            final OfflinePlayer offlinePlayer = PaperUtil.getOfflinePlayer(name);
            if (offlinePlayer != null) {
                profileCache.add(offlinePlayer.getUniqueId(), offlinePlayer.getName());
            }
            return offlinePlayer == null ? null : profileCache.getProfile(name);
        }
    }

    public static CompletableFuture<Profile> lookupProfileByName(final String name) {
        if (name == null || name.isEmpty() || NIL_UUID_STRING.equals(name)) {
            return CompletableFuture.completedFuture(SimpleProfileCache.EMPTY_PROFILE);
        }
        final PlayerProfile playerProfile = Bukkit.createPlayerProfile(name);
        final CompletableFuture<PlayerProfile> updatedProfile = playerProfile.update();
        final ProfileCache profileCache = JavaPlugin.getPlugin(BoltPlugin.class).getProfileCache();
        updatedProfile.thenAccept(profile -> {
            if (profile.isComplete()) {
                profileCache.add(profile.getUniqueId(), profile.getName());
            }
        });
        return updatedProfile.thenApplyAsync(PlayerProfile::getUniqueId, BukkitMainThreadExecutor.get()).thenApply(profileCache::getProfile);
    }

    public static CompletableFuture<Profile> findOrLookupProfileByName(final String name) {
        final Profile found = BukkitAdapter.findProfileByName(name);
        if (found != null) {
            return CompletableFuture.completedFuture(found);
        }
        return lookupProfileByName(name);
    }

    public static CompletableFuture<Collection<Profile>> findOrLookupProfilesByNames(final Collection<String> names) {
        final CompletableFuture<Collection<Profile>> profilesFuture = new CompletableFuture<>();
        final List<CompletableFuture<Profile>> profileFutures = new ArrayList<>();
        names.forEach(name -> profileFutures.add(BukkitAdapter.findOrLookupProfileByName(name)));
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
        if (uuid == null || NIL_UUID.equals(uuid)) {
            return CompletableFuture.completedFuture(SimpleProfileCache.EMPTY_PROFILE);
        }
        final PlayerProfile playerProfile = Bukkit.createPlayerProfile(uuid);
        final CompletableFuture<PlayerProfile> updatedProfile = playerProfile.update();
        final ProfileCache profileCache = JavaPlugin.getPlugin(BoltPlugin.class).getProfileCache();
        updatedProfile.thenAccept(profile -> {
            if (profile.isComplete()) {
                profileCache.add(profile.getUniqueId(), profile.getName());
            }
        });
        return updatedProfile.thenApplyAsync(PlayerProfile::getName, BukkitMainThreadExecutor.get()).thenApply(profileCache::getProfile);
    }

    public static CompletableFuture<Profile> findOrLookupProfileByUniqueId(final UUID uuid) {
        final Profile found = BukkitAdapter.findProfileByUniqueId(uuid);
        if (found != null) {
            return CompletableFuture.completedFuture(found);
        }
        return lookupProfileByUniqueId(uuid);
    }

    public static CompletableFuture<Collection<Profile>> findOrLookupProfilesByUniqueIds(final Collection<UUID> uuids) {
        final CompletableFuture<Collection<Profile>> profilesFuture = new CompletableFuture<>();
        final List<CompletableFuture<Profile>> profileFutures = new ArrayList<>();
        uuids.forEach(uuid -> profileFutures.add(BukkitAdapter.findOrLookupProfileByUniqueId(uuid)));
        CompletableFuture.allOf(profileFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
            final List<Profile> profiles = new ArrayList<>();
            profileFutures.forEach(profileFuture -> profiles.add(profileFuture.join()));
            profilesFuture.complete(profiles);
        });
        return profilesFuture;
    }
}

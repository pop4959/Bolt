package org.popcraft.bolt.data.migration.lockette;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.data.MemoryStore;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.util.BlockLocation;
import org.popcraft.bolt.util.ChunkPos;
import org.popcraft.bolt.util.PaperUtil;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.chunky.nbt.ByteTag;
import org.popcraft.chunky.nbt.CompoundTag;
import org.popcraft.chunky.nbt.IntTag;
import org.popcraft.chunky.nbt.ListTag;
import org.popcraft.chunky.nbt.LongArrayTag;
import org.popcraft.chunky.nbt.StringTag;
import org.popcraft.chunky.nbt.Tag;
import org.popcraft.chunky.nbt.util.RegionFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

public class LocketteMigration {
    private final BoltPlugin plugin;

    public LocketteMigration(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<MemoryStore> convertAsync() {
        return CompletableFuture.supplyAsync(this::convert);
    }

    private MemoryStore convert() {
        final MemoryStore store = new MemoryStore();
        Bukkit.getServer().getWorlds().forEach(world -> {
            final Optional<Path> regionDirectory = findRegionDirectory(world);
            if (regionDirectory.isPresent()) {
                final List<LocketteProtection> locketteProtections = new ArrayList<>();
                try (final Stream<Path> regionWalker = Files.walk(regionDirectory.get())) {
                    regionWalker.filter(path -> {
                        final String fileName = path.getFileName().toString();
                        return fileName.startsWith("r.") && fileName.endsWith(".mca");
                    }).forEach(region -> {
                        final RegionFile regionFile = new RegionFile(region.toFile());
                        regionFile.getChunks().forEach(chunk -> chunk.getData().getList("block_entities")
                                .map(ListTag::value)
                                .ifPresent(blockEntityTags -> {
                                    for (final Tag tag : blockEntityTags) {
                                        if (!(tag instanceof final CompoundTag blockEntityCompound)) {
                                            continue;
                                        }
                                        final String id = blockEntityCompound.getString("id").map(StringTag::value).orElse(null);
                                        if (!"minecraft:sign".equals(id)) {
                                            continue;
                                        }
                                        final LocketteProtection pdcProtection = fromPersistentData(blockEntityCompound);
                                        if (pdcProtection != null) {
                                            locketteProtections.add(pdcProtection);
                                        } else {
                                            final LocketteProtection messagesProtection = fromSignMessages(blockEntityCompound);
                                            if (messagesProtection != null) {
                                                locketteProtections.add(messagesProtection);
                                            }
                                        }
                                    }
                                }));
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final int permits = 10;
                final Semaphore working = new Semaphore(permits);
                for (final LocketteProtection locketteProtection : locketteProtections) {
                    final int chunkX = locketteProtection.x() >> 4;
                    final int chunkZ = locketteProtection.z() >> 4;
                    final ChunkPos chunkPos = new ChunkPos(world.getName(), chunkX, chunkZ);
                    try {
                        working.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    world.getChunkAtAsync(chunkX, chunkZ)
                            .thenAccept(ignored -> {
                                final Block signBlock = world.getBlockAt(locketteProtection.x(), locketteProtection.y(), locketteProtection.z());
                                if (signBlock.getBlockData() instanceof final WallSign wallSign) {
                                    final BlockFace facing = wallSign.getFacing();
                                    final Block block = signBlock.getRelative(facing.getOppositeFace());
                                    final BlockLocation blockLocation = new BlockLocation(world.getName(), block.getX(), block.getY(), block.getZ());
                                    final BlockProtection existing = store.loadBlockProtection(blockLocation).join();
                                    if (existing == null) {
                                        final BlockProtection protection = plugin.createProtection(block, locketteProtection.owner(), locketteProtection.type());
                                        protection.setAccess(locketteProtection.access());
                                        store.saveBlockProtection(protection);
                                    } else {
                                        if (Profiles.NIL_UUID.equals(existing.getOwner())) {
                                            existing.setOwner(locketteProtection.owner());
                                        }
                                        existing.getAccess().putAll(locketteProtection.access());
                                        if ("public".equals(locketteProtection.type())) {
                                            existing.setType("public");
                                        }
                                        store.saveBlockProtection(existing);
                                    }
                                }
                            })
                            .thenRun(working::release);
                }
                working.acquireUninterruptibly(permits);
            }
        });
        return store;
    }

    private LocketteProtection fromPersistentData(final CompoundTag sign) {
        final Integer x = sign.getInt("x").map(IntTag::value).orElse(null);
        final Integer y = sign.getInt("y").map(IntTag::value).orElse(null);
        final Integer z = sign.getInt("z").map(IntTag::value).orElse(null);
        if (x == null || y == null || z == null) {
            return null;
        }
        final CompoundTag pdcCompound = sign.getCompound("PublicBukkitValues")
                .orElse(null);
        if (pdcCompound == null) {
            return null;
        }
        final String header = pdcCompound.getString("blocklocker:header")
                .map(StringTag::value)
                .orElse(null);
        if (header == null) {
            return null;
        }
        boolean isPublic = false;
        boolean isRedstone = false;
        UUID owner = null;
        final Map<String, String> access = new HashMap<>();
        for (int i = 0; i < 6; ++i) {
            final int profileId = i + 1;
            final CompoundTag profileCompound = pdcCompound.getCompound("blocklocker:profile_%d".formatted(profileId))
                    .orElse(null);
            if (profileCompound == null) {
                continue;
            }
            // Player profile name
            final String n = profileCompound.getString("blocklocker:n")
                    .map(StringTag::value)
                    .orElse(null);
            // Player profile uuid
            final long[] u = profileCompound.getLongArray("blocklocker:u")
                    .map(LongArrayTag::value)
                    .orElse(null);
            // Everyone
            final Byte e = profileCompound.getByte("blocklocker:e")
                    .map(ByteTag::value)
                    .orElse(null);
            // Group leader
            final String l = profileCompound.getString("blocklocker:l")
                    .map(StringTag::value)
                    .orElse(null);
            // Group
            final String g = profileCompound.getString("blocklocker:g")
                    .map(StringTag::value)
                    .orElse(null);
            // Redstone
            final Byte r = profileCompound.getByte("blocklocker:r")
                    .map(ByteTag::value)
                    .orElse(null);
            // Timer
            final Integer t = profileCompound.getInt("blocklocker:t")
                    .map(IntTag::value)
                    .orElse(null);
            if (n != null) {
                // Player profile
                final UUID uuid;
                if (u == null) {
                    uuid = Profiles.findOrLookupProfileByName(n).join().uuid();
                } else {
                    uuid = new UUID(u[0], u[1]);
                }
                if (uuid != null) {
                    if ("PRIVATE".equals(header) && owner == null) {
                        owner = uuid;
                    } else {
                        access.put(Source.player(uuid).toString(), "normal");
                    }
                }
            } else if (e != null && e == 1) {
                // Everyone
                isPublic = true;
            } else if (l != null) {
                // Group leader
                access.put(Source.of("permission", "group.%s".formatted(l)).toString(), "normal");
            } else if (g != null) {
                // Group
                access.put(Source.of("permission", "group.%s".formatted(g)).toString(), "normal");
            } else if (r != null && r == 1) {
                // Redstone
                isRedstone = true;
            } else if (t != null) {
                // Timer
                access.put(Source.of("door").toString(), "autoclose");
            }
        }
        final String type = isPublic ? "public" : "private";
        return new LocketteProtection(x, y, z, Objects.requireNonNullElse(owner, Profiles.NIL_UUID), type, access);
    }

    private LocketteProtection fromSignMessages(final CompoundTag sign) {
        final Integer x = sign.getInt("x").map(IntTag::value).orElse(null);
        final Integer y = sign.getInt("y").map(IntTag::value).orElse(null);
        final Integer z = sign.getInt("z").map(IntTag::value).orElse(null);
        if (x == null || y == null || z == null) {
            return null;
        }
        final List<String> messages = new ArrayList<>(sign.getCompound("front_text")
                .flatMap(compound -> compound.getList("messages"))
                .map(ListTag::value)
                .map(list -> {
                    final List<String> messageList = new ArrayList<>();
                    for (final Tag tag : list) {
                        if (tag instanceof final StringTag message && !message.value().isBlank()) {
                            messageList.add(message.value());
                        }
                    }
                    return messageList;
                })
                .orElse(List.of()));
        messages.addAll(sign.getCompound("back_text")
                .flatMap(compound -> compound.getList("messages"))
                .map(ListTag::value)
                .map(list -> {
                    final List<String> messageList = new ArrayList<>();
                    for (final Tag tag : list) {
                        if (tag instanceof final StringTag message && !message.value().isBlank()) {
                            messageList.add(message.value());
                        }
                    }
                    return messageList;
                })
                .orElse(List.of()));
        boolean isValid = false;
        boolean isPublic = false;
        boolean isRedstone = false;
        boolean hasPrivateHeader = false;
        UUID owner = null;
        final Map<String, String> access = new HashMap<>();
        for (final String message : messages) {
            final String cleaned = message.replaceAll("\"", "");
            final boolean privateHeader = cleaned.contains("[Private]");
            final boolean moreUsersHeader = cleaned.contains("[More Users]");
            if (privateHeader || moreUsersHeader) {
                isValid = true;
                hasPrivateHeader = privateHeader;
            } else if (cleaned.contains("[Everyone]")) {
                isPublic = true;
            } else if (cleaned.contains("[Redstone]")) {
                isRedstone = true;
            } else if (cleaned.contains("Timer")) {
                access.put(Source.of("door").toString(), "autoclose");
            } else {
                UUID uuid;
                final boolean isLocketteProFormat = cleaned.contains("#");
                if (isLocketteProFormat) {
                    final int uuidStart = cleaned.indexOf("#") + 1;
                    try {
                        final String name = cleaned.substring(uuidStart).trim().replace(" ", "");
                        uuid = UUID.fromString(name);
                    } catch (IllegalArgumentException ignore) {
                        uuid = null;
                    }
                } else {
                    final String name = cleaned.trim().replace(" ", "");
                    uuid = Profiles.findOrLookupProfileByName(name).join().uuid();
                }
                if (uuid != null) {
                    if (hasPrivateHeader && owner == null) {
                        owner = uuid;
                    } else {
                        access.put(Source.player(uuid).toString(), "normal");
                    }
                }
            }
        }
        if (!isValid) {
            return null;
        }
        final String type = isPublic ? "public" : "private";
        return new LocketteProtection(x, y, z, Objects.requireNonNullElse(owner, Profiles.NIL_UUID), type, access);
    }

    private Optional<Path> findRegionDirectory(final World world) {
        try (final Stream<Path> paths = Files.walk(world.getWorldFolder().toPath())) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> "region".equals(path.getFileName().toString()))
                    .findFirst();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private record LocketteProtection(int x, int y, int z, UUID owner, String type, Map<String, String> access) {
    }
}

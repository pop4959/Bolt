package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.util.Vector;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.access.Access;
import org.popcraft.bolt.lang.Translation;
import org.popcraft.bolt.matcher.Match;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.Source;
import org.popcraft.bolt.source.SourceResolver;
import org.popcraft.bolt.source.SourceTypeResolver;
import org.popcraft.bolt.source.SourceTypes;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.Doors;
import org.popcraft.bolt.util.EnumUtil;
import org.popcraft.bolt.util.Mode;
import org.popcraft.bolt.util.PaperUtil;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.ProtectableBlock;
import org.popcraft.bolt.util.ProtectableConfig;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.EnumSet;
import java.util.List;

import static org.popcraft.bolt.util.BoltComponents.translateRaw;

public final class BlockListener extends AbstractInteractionListener<ProtectableBlock> implements Listener {
    private static final SourceResolver REDSTONE_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.REDSTONE));
    private static final EnumSet<Material> DYES = EnumSet.of(Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE, Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE, Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE);
    private static final Material CHISELED_BOOKSHELF = EnumUtil.valueOf(Material.class, "CHISELED_BOOKSHELF").orElse(null);
    private static final Material SCULK_SENSOR = EnumUtil.valueOf(Material.class, "SCULK_SENSOR").orElse(null);
    private static final Material CALIBRATED_SCULK_SENSOR = EnumUtil.valueOf(Material.class, "CALIBRATED_SCULK_SENSOR").orElse(null);
    private static final Material DECORATED_POT = EnumUtil.valueOf(Material.class, "DECORATED_POT").orElse(null);

    public BlockListener(final BoltPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (org.bukkit.event.block.Action.LEFT_CLICK_AIR.equals(e.getAction()) || org.bukkit.event.block.Action.RIGHT_CLICK_AIR.equals(e.getAction()) || org.bukkit.event.block.Action.PHYSICAL.equals(e.getAction())) {
            return;
        }
        final Block clicked = e.getClickedBlock();
        if (clicked == null) {
            return;
        }
        final Player player = e.getPlayer();
        final BoltPlayer boltPlayer = plugin.player(player);
        if (boltPlayer.hasInteracted()) {
            if (boltPlayer.isInteractionCancelled()) {
                e.setCancelled(true);
            }
            return;
        }
        final Protection protection = plugin.findProtection(clicked);
        boolean shouldCancel = false;
        if (triggerActions(player, protection, new ProtectableBlock(clicked))) {
            boltPlayer.setInteracted(true);
            SchedulerUtil.schedule(plugin, player, boltPlayer::clearInteraction);
            shouldCancel = true;
        } else if (protection != null) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            final boolean canInteract = plugin.canAccess(protection, player, Permission.INTERACT);
            if (canInteract && protection instanceof final BlockProtection blockProtection) {
                protection.setAccessed(System.currentTimeMillis());
                plugin.saveProtection(blockProtection);
            }
            if (!canInteract) {
                shouldCancel = true;
                if (!hasNotifyPermission) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.LOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                    );
                }
            }
            if (plugin.isDoors() && canInteract) {
                Doors.handlePlayerInteract(plugin, e);
            }
            if (hasNotifyPermission) {
                Profiles.findOrLookupProfileByUniqueId(protection.getOwner()).thenAccept(profile -> {
                    final boolean noSpam = plugin.player(player.getUniqueId()).hasMode(Mode.NOSPAM);
                    if (noSpam) {
                        return;
                    }
                    final boolean isYou = player.getUniqueId().equals(protection.getOwner());
                    final String owner = isYou ? translateRaw(Translation.YOU, player) : profile.name();
                    if (owner == null) {
                        SchedulerUtil.schedule(plugin, player, () -> {
                            if (!plugin.isProtected(clicked)) {
                                return;
                            }
                            BoltComponents.sendMessage(
                                    player,
                                    Translation.PROTECTION_NOTIFY_GENERIC,
                                    plugin.isUseActionBar(),
                                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player)),
                                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                            );
                        });
                    } else if (!isYou || player.hasPermission("bolt.protection.notify.self")) {
                        SchedulerUtil.schedule(plugin, player, () -> {
                            if (!plugin.isProtected(clicked)) {
                                return;
                            }
                            BoltComponents.sendMessage(
                                    player,
                                    Translation.PROTECTION_NOTIFY,
                                    plugin.isUseActionBar(),
                                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player)),
                                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player)),
                                    Placeholder.component(Translation.Placeholder.PLAYER, Component.text(owner))
                            );
                        });
                    }
                });
            }
            if (e.getItem() != null) {
                final Material itemType = e.getItem().getType();
                if (Material.LECTERN.equals(clicked.getType()) && (Material.WRITABLE_BOOK.equals(itemType) || Material.WRITTEN_BOOK.equals(itemType)) && !plugin.canAccess(protection, player, Permission.DEPOSIT)) {
                    e.setUseItemInHand(Event.Result.DENY);
                } else if ((Tag.SIGNS.isTagged(clicked.getType()) && (DYES.contains(itemType) || Material.GLOW_INK_SAC.equals(itemType)) && !plugin.canAccess(protection, player, Permission.INTERACT))) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
            }
            if (CHISELED_BOOKSHELF != null && CHISELED_BOOKSHELF.equals(clicked.getType()) && clicked.getState() instanceof final ChiseledBookshelf chiseledBookshelf && clicked.getBlockData() instanceof final org.bukkit.block.data.type.ChiseledBookshelf chiseledBookshelfBlockData && chiseledBookshelfBlockData.getFacing() == e.getBlockFace()) {
                // Future: Replace with Material.CHISELED_BOOKSHELF
                final Vector clickedPosition = PaperUtil.getClickedPosition(e);
                if (clickedPosition != null) {
                    final int slot = chiseledBookshelf.getSlot(clickedPosition);
                    final boolean isOccupied = chiseledBookshelfBlockData.isSlotOccupied(slot);
                    if ((!isOccupied && !plugin.canAccess(protection, player, Permission.DEPOSIT)) || (isOccupied && !plugin.canAccess(protection, player, Permission.WITHDRAW))) {
                        e.setUseItemInHand(Event.Result.DENY);
                        e.setUseInteractedBlock(Event.Result.DENY);
                    }
                }
            }
            if (DECORATED_POT != null && DECORATED_POT.equals(clicked.getType()) && e.getItem() != null) {
                if (!plugin.canAccess(protection, player, Permission.DEPOSIT)) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
            }
            boltPlayer.setInteracted(shouldCancel);
            SchedulerUtil.schedule(plugin, player, boltPlayer::clearInteraction);
        }
        if (shouldCancel) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(final BlockPlaceEvent e) {
        final Player player = e.getPlayer();
        if (plugin.player(player.getUniqueId()).hasMode(Mode.NOLOCK)) {
            return;
        }
        final Block block = e.getBlock();
        if (!plugin.isProtectable(block)) {
            return;
        }
        final ProtectableConfig protectableConfig = plugin.getProtectableConfig(block);
        if (protectableConfig == null) {
            return;
        }
        if (protectableConfig.autoProtectPermission() && !player.hasPermission("bolt.protection.autoprotect.%s".formatted(block.getType().name().toLowerCase()))) {
            return;
        }
        final Access access = protectableConfig.defaultAccess();
        if (access == null) {
            return;
        }
        if (access.restricted() && !player.hasPermission("bolt.type.protection.%s".formatted(access.type()))) {
            return;
        }
        if (plugin.isProtected(block)) {
            return;
        }
        final BlockProtection newProtection = plugin.createProtection(block, player.getUniqueId(), access.type());
        plugin.saveProtection(newProtection);
        if (!plugin.player(player.getUniqueId()).hasMode(Mode.NOSPAM)) {
            BoltComponents.sendMessage(
                    player,
                    Translation.CLICK_LOCKED,
                    plugin.isUseActionBar(),
                    Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(newProtection, player)),
                    Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(block, player))
            );
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Protection protection = plugin.findProtection(block);
        final Player player = e.getPlayer();
        if (protection == null) {
            return;
        }
        if (plugin.canAccess(protection, player, Permission.DESTROY)) {
            if (protection instanceof final BlockProtection blockProtection) {
                // Double chests are a special case where we want to move the protection instead
                if (plugin.getChestMatcher().canMatch(block)) {
                    if (!plugin.isProtectedExact(block)) {
                        return;
                    }
                    final Match match = plugin.getChestMatcher().findMatch(block);
                    final Block newBlock = match == null ? null : match.blocks().stream().findAny().orElse(null);
                    if (newBlock != null) {
                        blockProtection.setX(newBlock.getX());
                        blockProtection.setY(newBlock.getY());
                        blockProtection.setZ(newBlock.getZ());
                        plugin.saveProtection(blockProtection);
                        return;
                    }
                }
                plugin.removeProtection(protection);
                if (!plugin.player(player.getUniqueId()).hasMode(Mode.NOSPAM)) {
                    BoltComponents.sendMessage(
                            player,
                            Translation.CLICK_UNLOCKED,
                            plugin.isUseActionBar(),
                            Placeholder.component(Translation.Placeholder.PROTECTION_TYPE, Protections.protectionType(protection, player)),
                            Placeholder.component(Translation.Placeholder.PROTECTION, Protections.displayType(protection, player))
                    );
                }
            }
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractPhysical(final PlayerInteractEvent e) {
        if (!org.bukkit.event.block.Action.PHYSICAL.equals(e.getAction())) {
            return;
        }
        final Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        final boolean isPressurePlate = Tag.PRESSURE_PLATES.isTagged(block.getType());
        if (!plugin.canAccess(block, e.getPlayer(), isPressurePlate ? Permission.INTERACT : Permission.DESTROY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignChange(final SignChangeEvent e) {
        if (!plugin.canAccess(e.getBlock(), e.getPlayer(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(final StructureGrowEvent e) {
        final Block growingBlock = e.getLocation().getBlock();
        if (plugin.isProtected(growingBlock)) {
            e.setCancelled(true);
            return;
        }
        e.getBlocks().removeIf(blockState -> plugin.isProtected(blockState.getBlock()));
    }

    @EventHandler
    public void onBlockMultiPlace(final BlockMultiPlaceEvent e) {
        for (final BlockState blockState : e.getReplacedBlockStates()) {
            if (plugin.isProtected(blockState.getBlock())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(final BlockFromToEvent e) {
        if (plugin.isProtected(e.getToBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(final BlockFadeEvent e) {
        if (plugin.isProtected(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(final BlockPistonRetractEvent e) {
        for (final Block block : e.getBlocks()) {
            if (plugin.isProtected(block)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockPistonExtend(final BlockPistonExtendEvent e) {
        final List<Block> blocks = e.getBlocks();
        for (final Block block : blocks) {
            if (plugin.isProtected(block)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockExplode(final BlockExplodeEvent e) {
        e.blockList().removeIf(plugin::isProtected);
    }

    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent e) {
        e.blockList().removeIf(plugin::isProtected);
    }

    @EventHandler
    public void onSpongeAbsorb(final SpongeAbsorbEvent e) {
        if (plugin.isProtected(e.getBlock())) {
            e.setCancelled(true);
            return;
        }
        e.getBlocks().removeIf(blockState -> plugin.isProtected(blockState.getBlock()));
    }

    @EventHandler
    public void onBlockIgnite(final BlockIgniteEvent e) {
        final Protection protection = plugin.findProtection(e.getBlock());
        if (protection == null) {
            return;
        }
        final Player player = e.getPlayer();
        if (player == null || !plugin.canAccess(protection, player, Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketEmpty(final PlayerBucketEmptyEvent e) {
        final Player player = e.getPlayer();
        if (!plugin.canAccess(e.getBlockClicked(), player, Permission.INTERACT) || !plugin.canAccess(e.getBlock(), player, Permission.DESTROY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketFill(final PlayerBucketFillEvent e) {
        final Player player = e.getPlayer();
        if (!plugin.canAccess(e.getBlockClicked(), player, Permission.INTERACT) || !plugin.canAccess(e.getBlock(), player, Permission.DESTROY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockForm(final BlockFormEvent e) {
        if (plugin.isProtected(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(final BlockSpreadEvent e) {
        if (plugin.isProtected(e.getBlock())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(final LeavesDecayEvent e) {
        final Block block = e.getBlock();
        if (plugin.isProtected(e.getBlock())) {
            if (block.getBlockData() instanceof final Leaves leaves) {
                leaves.setPersistent(true);
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockRedstone(final BlockRedstoneEvent e) {
        final Protection protection = plugin.findProtection(e.getBlock());
        if (protection == null) {
            return;
        }
        if (!plugin.canAccess(protection, REDSTONE_SOURCE_RESOLVER, Permission.REDSTONE)) {
            e.setNewCurrent(e.getOldCurrent());
        }
    }

    @EventHandler
    public void onPlayerTakeLecternBook(final PlayerTakeLecternBookEvent e) {
        if (!plugin.canAccess(e.getLectern().getBlock(), e.getPlayer(), Permission.WITHDRAW)) {
            e.setCancelled(true);
        }
    }

    public void onPlayerRecipeBookClick(final PlayerEvent e) {
        if (!(e instanceof Cancellable cancellable)) {
            return;
        }
        final Player player = e.getPlayer();
        final Location location = player.getOpenInventory().getTopInventory().getLocation();
        if (location != null && !plugin.canAccess(location.getBlock(), player, Permission.DEPOSIT, Permission.WITHDRAW)) {
            cancellable.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockDispense(final BlockDispenseEvent e) {
        final Block block = e.getBlock();
        final Protection existingProtection = plugin.findProtection(block);
        if (existingProtection == null) {
            return;
        }
        if (!plugin.canAccess(existingProtection, REDSTONE_SOURCE_RESOLVER, Permission.REDSTONE)) {
            e.setCancelled(true);
            return;
        }
        final BlockData blockData = block.getBlockData();
        if (!(blockData instanceof final Directional directional)) {
            return;
        }
        final Block placing = block.getRelative(directional.getFacing());
        if (!Material.AIR.equals(placing.getType())) {
            return;
        }
        final Material placingType = e.getItem().getType();
        SchedulerUtil.schedule(plugin, block.getLocation(), () -> {
            final Block placed = placing.getWorld().getBlockAt(placing.getLocation());
            if (!placed.getType().equals(placingType)) {
                return;
            }
            if (!plugin.isProtectable(placed)) {
                return;
            }
            final BlockProtection newProtection = plugin.createProtection(placed, existingProtection.getOwner(), existingProtection.getType());
            plugin.saveProtection(newProtection);
        });
    }

    public void onBlockPreDispense(final BlockEvent e) {
        if (!(e instanceof Cancellable cancellable)) {
            return;
        }
        final Block block = e.getBlock();
        final Protection existingProtection = plugin.findProtection(block);
        if (existingProtection == null) {
            return;
        }
        if (!plugin.canAccess(existingProtection, REDSTONE_SOURCE_RESOLVER, Permission.REDSTONE)) {
            cancellable.setCancelled(true);
        }
    }

    // BlockDestroyEvent is called when a block is about to be destroyed. We use it here to prevent a protected block
    // from becoming destroyed involuntarily, for example, because it is a door and its support was removed. For other
    // cases, like just breaking the door itself, it will go through other events and be handled properly.
    // This is a last resort event.
    public void onBlockDestroy(final BlockEvent e) {
        if (!(e instanceof Cancellable cancellable)) {
            return;
        }
        final Block block = e.getBlock();
        final Protection existingProtection = plugin.findProtection(block);
        if (existingProtection == null) {
            return;
        }
        cancellable.setCancelled(true);
    }

    @EventHandler
    public void onBlockReceiveGameEvent(final BlockReceiveGameEvent e) {
        if (!(e.getEntity() instanceof final Player player)) {
            return;
        }
        final Material material = e.getBlock().getType();
        if ((SCULK_SENSOR == null || !SCULK_SENSOR.equals(material)) && (CALIBRATED_SCULK_SENSOR == null || !CALIBRATED_SCULK_SENSOR.equals(material))) {
            return;
        }
        final Protection protection = plugin.findProtection(e.getBlock());
        if (protection == null) {
            return;
        }
        if (!plugin.canAccess(protection, player, Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }
}

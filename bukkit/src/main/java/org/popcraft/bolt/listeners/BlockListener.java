package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.block.data.type.Piston;
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
import org.popcraft.bolt.util.Mode;
import org.popcraft.bolt.util.PaperUtil;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.Profiles;
import org.popcraft.bolt.util.ProtectableConfig;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.SchedulerUtil;

import java.util.List;
import java.util.Set;

import static org.popcraft.bolt.util.BoltComponents.translateRaw;

public final class BlockListener extends InteractionListener implements Listener {
    private static final SourceResolver REDSTONE_SOURCE_RESOLVER = new SourceTypeResolver(Source.of(SourceTypes.REDSTONE));
    private static final Set<Material> DYES = Set.of(Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE, Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE, Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE);

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
        if (triggerAction(player, protection, clicked)) {
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
            if (Material.CHISELED_BOOKSHELF.equals(clicked.getType()) && clicked.getState() instanceof final ChiseledBookshelf chiseledBookshelf && clicked.getBlockData() instanceof final org.bukkit.block.data.type.ChiseledBookshelf chiseledBookshelfBlockData && chiseledBookshelfBlockData.getFacing() == e.getBlockFace()) {
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
            if (Material.DECORATED_POT.equals(clicked.getType()) && e.getItem() != null) {
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

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        final BlockState replaced = e.getBlockReplacedState();
        if (replaced.getType().isAir()) {
            return;
        }
        final Block block = replaced.getBlock();
        final Protection protection = plugin.findProtection(block);
        if (protection == null) {
            return;
        }
        if (Tag.REPLACEABLE.isTagged(e.getBlockReplacedState().getType())) {
            // Prevent accidental deletion of protected blocks by them getting replaced.
            // Purposefully not checking for destroy permissions, that logic is for BlockBreakEvent.
            e.setCancelled(true);
        } else {
            // This is, bafflingly, fired in cases like stripping a log (which *also* called EntityChangeBlockEvent)
            // but also fired in other cases, like placing a slab on top of another.
            if (!plugin.canAccess(block, e.getPlayer(), Permission.INTERACT)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlaceMonitor(final BlockPlaceEvent e) {
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
        if (!plugin.canAccess(e.getBlock(), e.getPlayer(), Permission.DESTROY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakMonitor(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Protection protection = plugin.findProtection(block);
        final Player player = e.getPlayer();
        if (!(protection instanceof final BlockProtection blockProtection)) {
            return;
        }
        // If we enter this event, it means we already passed the permission check, so all that's left is to clean
        // up the protection.

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
        final Protection pistonProtection = plugin.loadProtection(e.getBlock());
        final List<Block> blocks = e.getBlocks();
        for (final Block block : blocks) {
            final Protection blockProtection = plugin.findProtection(block);
            if (blockProtection != null) {
                // Check for pistons breaking breakable things. Guarded by a paper check because BlockBreakBlockEvent is
                // paper only, and we need to clean up the protection.
                final boolean canBreak = pistonProtection != null && PaperUtil.isPaper()
                        && block.getPistonMoveReaction() == PistonMoveReaction.BREAK
                        && plugin.canAccess(blockProtection, pistonProtection.getOwner(), Permission.DESTROY);

                // Either something that won't be broken, or something the piston isn't allowed to break. Either way,
                // don't allow the piston to move.
                if (!canBreak) {
                    e.setCancelled(true);
                    return;
                }
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
        final Protection protection = plugin.findProtection(e.getBlock());
        if (protection == null) {
            return;
        }
        final Player player = e.getPlayer();
        if (e.getBlock().equals(e.getBlockClicked()) && Tag.CAULDRONS.isTagged(e.getBlock().getType())) {
            if (!plugin.canAccess(protection, player, Permission.INTERACT, Permission.DEPOSIT)) {
                e.setCancelled(true);
            }
        } else if (!e.getBlock().equals(e.getBlockClicked())) {
            // Prevent accidental deletion of protected blocks by them getting replaced.
            // Purposefully not checking for destroy permissions, that logic is for BlockBreakEvent.
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerBucketFill(final PlayerBucketFillEvent e) {
        final Protection protection = plugin.findProtection(e.getBlock());
        if (protection == null) {
            return;
        }
        final Player player = e.getPlayer();
        if (e.getBlock().equals(e.getBlockClicked()) && Tag.CAULDRONS.isTagged(e.getBlock().getType())) {
            if (!plugin.canAccess(protection, player, Permission.INTERACT, Permission.WITHDRAW)) {
                e.setCancelled(true);
            }
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
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispenseMonitor(final BlockDispenseEvent e) {
        final Block block = e.getBlock();
        final Protection existingProtection = plugin.findProtection(block);
        if (existingProtection == null) {
            return;
        }
        final Material placingType = e.getItem().getType();
        if (Tag.SHULKER_BOXES.isTagged(placingType) && plugin.isProtectable(placingType) && block.getBlockData() instanceof Directional directional) {
            // Dispensing a shulker places it. If the dispenser was locked, transfer the owner to the shulker.
            // This event doesn't let us access the placed block, so we end up creating the protection before the block actually exists, by anticipating where it will be placed.
            final Block placeTo = block.getRelative(directional.getFacing());
            if (!placeTo.getType().isAir()) {
                return;
            }
            final BlockProtection newProtection = plugin.createProtection(placeTo, existingProtection.getOwner(), existingProtection.getType());
            newProtection.setBlock(placingType.name());
            plugin.saveProtection(newProtection);
        }
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

    // Called when a piston breaks a block. We can only reach here if BlockPistonExtendEvent allowed the
    // interaction to happen, so all we need to do here is clean up. Executed at MONITOR priority.
    public void onBlockBreakBlockEvent(final BlockEvent e, final Block source) {
        final Block target = e.getBlock();
        final Protection targetProtection = plugin.findProtection(target);
        if (source.getBlockData() instanceof Piston && targetProtection != null) {
            plugin.removeProtection(targetProtection);
        }
    }

    @EventHandler
    public void onBlockReceiveGameEvent(final BlockReceiveGameEvent e) {
        if (!(e.getEntity() instanceof final Player player)) {
            return;
        }
        final Material material = e.getBlock().getType();
        if (!Material.SCULK_SENSOR.equals(material) && !Material.CALIBRATED_SCULK_SENSOR.equals(material)) {
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

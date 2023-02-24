package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.util.BoundingBox;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Access;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BasicPermissible;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BoltPlayer;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Mode;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.Source;
import org.popcraft.bolt.lang.Strings;
import org.popcraft.bolt.lang.Translation;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.popcraft.bolt.lang.Translator.translate;
import static org.popcraft.bolt.util.BukkitAdapter.NIL_UUID;

public final class BlockListener implements Listener {
    private static final EnumSet<Material> DYES = EnumSet.of(Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE, Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE, Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE);
    private final BoltPlugin plugin;

    public BlockListener(final BoltPlugin plugin) {
        this.plugin = plugin;
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
            e.setCancelled(true);
            return;
        }
        final Protection protection = plugin.findProtection(clicked).orElse(null);
        if (triggerActions(player, protection, clicked)) {
            boltPlayer.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, boltPlayer::clearInteraction);
            e.setCancelled(true);
        } else if (protection != null) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            if (!plugin.canAccess(protection, player, Permission.INTERACT)) {
                e.setCancelled(true);
                if (!hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Placeholder.unparsed("type", Protections.displayType(protection)));
                }
            }
            if (hasNotifyPermission) {
                final boolean isYou = player.getUniqueId().equals(protection.getOwner());
                final String owner = isYou ? translate(Translation.YOU) : plugin.getProfileCache().getName(protection.getOwner());
                if (owner == null) {
                    BukkitAdapter.lookupPlayerName(protection.getOwner());
                    BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY_GENERIC, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("type", Protections.displayType(protection)));
                } else {
                    BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("type", Protections.displayType(protection)), Placeholder.unparsed("owner", owner));
                }
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
            boltPlayer.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, boltPlayer::clearInteraction);
        }
    }

    private boolean triggerActions(final Player player, final Protection protection, final Block block) {
        final BoltPlayer boltPlayer = plugin.player(player);
        final Action action = boltPlayer.getAction();
        if (action == null) {
            return false;
        }
        final Action.Type actionType = action.getType();
        switch (actionType) {
            case LOCK -> {
                final String protectionType = Optional.ofNullable(action.getData())
                        .flatMap(type -> plugin.getBolt().getAccessRegistry().getProtectionByType(type))
                        .map(Access::type)
                        .orElse(plugin.getDefaultProtectionType());
                if (protection != null) {
                    if (player.getUniqueId() == protection.getOwner() && !protection.getType().equals(protectionType)) {
                        protection.setType(protectionType);
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_CHANGED, Placeholder.unparsed("type", protectionType));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY, Placeholder.unparsed("type", Protections.displayType(protection)));
                    }
                } else if (plugin.isProtectable(block)) {
                    final BlockProtection newProtection = BukkitAdapter.createBlockProtection(block, boltPlayer.isLockNil() ? NIL_UUID : player.getUniqueId(), protectionType);
                    plugin.getBolt().getStore().saveBlockProtection(newProtection);
                    boltPlayer.setLockNil(false);
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED, Placeholder.unparsed("access", Strings.toTitleCase(newProtection.getType())), Placeholder.unparsed("type", Protections.displayType(block)));
                } else {
                    return true;
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    if (plugin.canAccess(protection, player, Permission.DESTROY)) {
                        plugin.removeProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("type", Protections.displayType(protection)));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED_NO_PERMISSION);
                    }
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Placeholder.unparsed("type", Protections.displayType(block)));
                }
            }
            case INFO -> {
                if (protection != null) {
                    final String owner = Objects.requireNonNullElseGet(plugin.getProfileCache().getName(protection.getOwner()), () -> {
                        BukkitAdapter.lookupPlayerName(protection.getOwner());
                        return translate(Translation.UNKNOWN);
                    });
                    BoltComponents.sendMessage(player, Translation.INFO, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("type", Protections.displayType(protection)), Placeholder.unparsed("owner", owner), Placeholder.unparsed("access_count", String.valueOf(protection.getAccess().size())), Placeholder.unparsed("access_list", Protections.accessList(protection)));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Placeholder.unparsed("type", Protections.displayType(block)));
                }
            }
            case EDIT -> {
                if (protection != null) {
                    if (plugin.canAccess(protection, player, Permission.EDIT)) {
                        boltPlayer.getModifications().forEach((source, type) -> {
                            if (Boolean.parseBoolean(action.getData())) {
                                protection.getAccess().put(source, type);
                            } else {
                                protection.getAccess().remove(source);
                            }
                        });
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("type", Protections.displayType(protection)));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_PERMISSION);
                    }
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Placeholder.unparsed("type", Protections.displayType(block)));
                }
                boltPlayer.getModifications().clear();
            }
            case DEBUG ->
                    BoltComponents.sendMessage(player, Optional.ofNullable(protection).map(Protection::toString).toString());
            case TRANSFER -> {
                if (protection != null) {
                    if (player.getUniqueId().equals(protection.getOwner())) {
                        final UUID uuid = UUID.fromString(action.getData());
                        protection.setOwner(uuid);
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_TRANSFER_CONFIRM, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("type", Protections.displayType(protection)), Placeholder.unparsed("owner", Optional.ofNullable(plugin.getProfileCache().getName(uuid)).orElse(translate(Translation.UNKNOWN))));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_OWNER);
                    }
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Placeholder.unparsed("type", Protections.displayType(block)));
                }
            }
        }
        boltPlayer.clearAction();
        return true;
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        final Player player = e.getPlayer();
        if (plugin.getBolt().getBoltPlayer(player.getUniqueId()).hasMode(Mode.NOLOCK)) {
            return;
        }
        final Block block = e.getBlock();
        if (!plugin.isProtectable(block)) {
            return;
        }
        final Access defaultAccess = plugin.getDefaultAccess(block);
        if (defaultAccess == null) {
            return;
        }
        if (plugin.findProtection(block).isPresent()) {
            return;
        }
        final BlockProtection newProtection = BukkitAdapter.createBlockProtection(block, player.getUniqueId(), defaultAccess.type());
        plugin.getBolt().getStore().saveBlockProtection(newProtection);
        if (!plugin.getBolt().getBoltPlayer(player.getUniqueId()).hasMode(Mode.NOSPAM)) {
            BoltComponents.sendMessage(player, Translation.CLICK_LOCKED, Placeholder.unparsed("access", Strings.toTitleCase(newProtection.getType())), Placeholder.unparsed("type", Protections.displayType(block)));
        }
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Optional<Protection> optionalProtection = plugin.findProtection(block);
        final Player player = e.getPlayer();
        if (optionalProtection.isPresent()) {
            final Protection protection = optionalProtection.get();
            if (plugin.canAccess(protection, player, Permission.DESTROY)) {
                plugin.removeProtection(protection);
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Placeholder.unparsed("access", Strings.toTitleCase(protection.getType())), Placeholder.unparsed("type", Protections.displayType(protection)));
            } else {
                e.setCancelled(true);
            }
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
        if (!plugin.canAccess(block, e.getPlayer(), Permission.DESTROY)) {
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
        if (plugin.findProtection(growingBlock).isPresent()) {
            e.setCancelled(true);
            return;
        }
        e.getBlocks().removeIf(blockState -> plugin.findProtection(blockState.getBlock()).isPresent());
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        if (plugin.findProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockMultiPlace(final BlockMultiPlaceEvent e) {
        for (final BlockState blockState : e.getReplacedBlockStates()) {
            if (plugin.findProtection(blockState.getBlock()).isPresent()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(final BlockFromToEvent e) {
        if (plugin.findProtection(e.getToBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(final BlockFadeEvent e) {
        if (plugin.findProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBreakDoor(final EntityBreakDoorEvent e) {
        if (plugin.findProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(final BlockPistonRetractEvent e) {
        for (final Block block : e.getBlocks()) {
            if (plugin.findProtection(block).isPresent()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockPistonExtend(final BlockPistonExtendEvent e) {
        final List<Block> blocks = e.getBlocks();
        for (final Block block : blocks) {
            if (plugin.findProtection(block).isPresent()) {
                e.setCancelled(true);
                return;
            }
            final BoundingBox moveArea = block.getBoundingBox().shift(e.getDirection().getDirection());
            if (!block.getWorld().getNearbyEntities(moveArea, entity -> plugin.findProtection(entity).isPresent()).isEmpty()) {
                e.setCancelled(true);
                return;
            }
        }
        if (blocks.isEmpty()) {
            final Block piston = e.getBlock();
            final BoundingBox moveArea = piston.getBoundingBox().shift(e.getDirection().getDirection());
            if (!piston.getWorld().getNearbyEntities(moveArea, entity -> plugin.findProtection(entity).isPresent()).isEmpty()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockExplode(final BlockExplodeEvent e) {
        e.blockList().removeIf(block -> plugin.findProtection(block).isPresent());
    }

    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent e) {
        e.blockList().removeIf(block -> plugin.findProtection(block).isPresent());
    }

    @EventHandler
    public void onSpongeAbsorb(final SpongeAbsorbEvent e) {
        if (plugin.findProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
            return;
        }
        e.getBlocks().removeIf(blockState -> plugin.findProtection(blockState.getBlock()).isPresent());
    }

    @EventHandler
    public void onBlockIgnite(final BlockIgniteEvent e) {
        plugin.findProtection(e.getBlock()).ifPresent(protection -> {
            final Player player = e.getPlayer();
            if (player == null || !plugin.canAccess(protection, player, Permission.INTERACT)) {
                e.setCancelled(true);
            }
        });
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
        if (plugin.findProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(final BlockSpreadEvent e) {
        if (plugin.findProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(final LeavesDecayEvent e) {
        final Block block = e.getBlock();
        if (plugin.findProtection(e.getBlock()).isPresent()) {
            if (block.getBlockData() instanceof final Leaves leaves) {
                leaves.setPersistent(true);
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockRedstone(final BlockRedstoneEvent e) {
        plugin.findProtection(e.getBlock()).ifPresent(protection -> {
            if (!plugin.canAccess(protection, new BasicPermissible(Source.from(Source.REDSTONE, Source.REDSTONE)), Permission.REDSTONE)) {
                e.setNewCurrent(e.getOldCurrent());
            }
        });
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
}

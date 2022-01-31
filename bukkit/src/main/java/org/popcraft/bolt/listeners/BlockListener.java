package org.popcraft.bolt.listeners;

import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import java.util.EnumSet;
import java.util.Optional;

import static org.popcraft.bolt.util.lang.Translator.translate;

@SuppressWarnings("ClassCanBeRecord")
public final class BlockListener implements Listener {
    private static final EnumSet<BlockFace> CARTESIAN_BLOCK_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
    private static final EnumSet<Material> DYES = EnumSet.of(Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE, Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE, Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE, Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE);
    // TODO: These uprooted types should be structures
    private static final EnumSet<Material> UPROOT = EnumSet.of(Material.BAMBOO, Material.CACTUS, Material.SUGAR_CANE);
    private final BoltPlugin plugin;

    public BlockListener(final BoltPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        final Block clicked = e.getClickedBlock();
        if (clicked == null) {
            return;
        }
        final Player player = e.getPlayer();
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        if (playerMeta.hasInteracted()) {
            e.setCancelled(true);
            return;
        }
        final BlockProtection protection = plugin.getBlockProtection(clicked).orElse(null);
        if (triggerActions(player, protection, clicked)) {
            playerMeta.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, playerMeta::clearInteraction);
            e.setCancelled(true);
        } else if (protection != null) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            if (!plugin.canAccessProtection(player, protection, Permission.INTERACT)) {
                e.setCancelled(true);
                if (!hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Template.of("type", Strings.toTitleCase(clicked.getType())));
                }
            }
            if (hasNotifyPermission) {
                BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY, Template.of("type", Strings.toTitleCase(clicked.getType())), Template.of("owner", player.getUniqueId().equals(protection.getOwner()) ? translate(Translation.YOU) : BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN))));
            }
            if (e.getItem() != null) {
                final Material itemType = e.getItem().getType();
                if (Material.LECTERN.equals(clicked.getType()) && (Material.WRITABLE_BOOK.equals(itemType) || Material.WRITTEN_BOOK.equals(itemType)) && !plugin.canAccessProtection(player, protection, Permission.DEPOSIT)) {
                    e.setUseItemInHand(Event.Result.DENY);
                } else if ((Tag.SIGNS.isTagged(clicked.getType()) && (DYES.contains(itemType) || Material.GLOW_INK_SAC.equals(itemType)) && !plugin.canAccessProtection(player, protection, Permission.MODIFY))) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
            }
            playerMeta.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, playerMeta::clearInteraction);
        }
    }

    @SuppressWarnings("java:S6205")
    private boolean triggerActions(final Player player, final BlockProtection protection, final Block block) {
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Action action = playerMeta.triggerAction();
        if (action == null) {
            return false;
        }
        switch (action) {
            case LOCK -> {
                if (protection != null) {
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY, Template.of("type", Strings.toTitleCase(block.getType())));
                } else {
                    plugin.getBolt().getStore().saveBlockProtection(BukkitAdapter.createPrivateBlockProtection(block, player));
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED, Template.of("type", Strings.toTitleCase(block.getType())));
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    plugin.getBolt().getStore().removeBlockProtection(protection);
                    BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Strings.toTitleCase(block.getType())));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(block.getType())));
                }
            }
            case INFO -> {
                if (protection != null) {
                    BoltComponents.sendMessage(player, Translation.INFO, Template.of("access", Strings.toTitleCase(protection.getType())), Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN))));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(block.getType())));
                }
            }
            case EDIT -> {
                if (protection != null) {
                    if (plugin.canAccessProtection(player, protection, Permission.EDIT)) {
                        playerMeta.getModifications().forEach((source, type) -> {
                            if (type == null || plugin.getBolt().getAccessRegistry().get(type).isEmpty()) {
                                protection.getAccess().remove(source);
                            } else {
                                protection.getAccess().put(source, type);
                            }
                        });
                        plugin.getBolt().getStore().saveBlockProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED, Template.of("type", Strings.toTitleCase(block.getType())));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_PERMISSION);
                    }
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Strings.toTitleCase(block.getType())));
                }
                playerMeta.getModifications().clear();
            }
            case DEBUG -> BoltComponents.sendMessage(player, Optional.ofNullable(protection).map(Protection::toString).toString());
        }
        return true;
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        final Block block = e.getBlock();
        final Material blockType = block.getType();
        final Player player = e.getPlayer();
        // TODO: move to matcher
        if (Material.CARVED_PUMPKIN.equals(blockType) || Material.JACK_O_LANTERN.equals(blockType)) {
            for (final BlockFace blockFace : CARTESIAN_BLOCK_FACES) {
                final Block firstBlock = block.getRelative(blockFace);
                final Block secondBlock = firstBlock.getRelative(blockFace);
                if (Material.SNOW_BLOCK.equals(firstBlock.getType()) && Material.SNOW_BLOCK.equals(secondBlock.getType())) {
                    final Optional<BlockProtection> firstProtection = plugin.getBlockProtection(firstBlock);
                    firstProtection.ifPresent(blockProtection -> {
                        if (plugin.canAccessProtection(player, blockProtection, Permission.BREAK)) {
                            plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                        } else {
                            e.setCancelled(true);
                        }
                    });
                    final Optional<BlockProtection> secondProtection = plugin.getBlockProtection(secondBlock);
                    secondProtection.ifPresent(blockProtection -> {
                        if (plugin.canAccessProtection(player, blockProtection, Permission.BREAK)) {
                            plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                        } else {
                            e.setCancelled(true);
                        }
                    });
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        final Block block = e.getBlock();
        final Material blockType = block.getType();
        final Optional<BlockProtection> optionalProtection = plugin.getBlockProtection(block);
        final Player player = e.getPlayer();
        if (optionalProtection.isPresent()) {
            final BlockProtection blockProtection = optionalProtection.get();
            if (plugin.canAccessProtection(player, blockProtection, Permission.BREAK)) {
                plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Strings.toTitleCase(block.getType())));
            } else {
                e.setCancelled(true);
            }
        } else if (UPROOT.contains(blockType)) {
            for (Block above = block.getRelative(BlockFace.UP); UPROOT.contains(above.getType()); above = above.getRelative(BlockFace.UP)) {
                final Optional<BlockProtection> optionalAboveProtection = plugin.getBlockProtection(above);
                if (optionalAboveProtection.isPresent()) {
                    final BlockProtection blockProtection = optionalAboveProtection.get();
                    if (plugin.canAccessProtection(player, blockProtection, Permission.BREAK)) {
                        plugin.getBolt().getStore().removeBlockProtection(blockProtection);
                    } else {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSignChange(final SignChangeEvent e) {
        if (!plugin.canAccessBlock(e.getPlayer(), e.getBlock(), Permission.MODIFY)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(final StructureGrowEvent e) {
        e.getBlocks().removeIf(blockState -> plugin.getBlockProtection(blockState.getBlock()).isPresent());
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        if (plugin.getBlockProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockMultiPlace(final BlockMultiPlaceEvent e) {
        for (final BlockState blockState : e.getReplacedBlockStates()) {
            if (plugin.getBlockProtection(blockState.getBlock()).isPresent()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(final BlockFromToEvent e) {
        if (plugin.getBlockProtection(e.getToBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(final BlockFadeEvent e) {
        if (plugin.getBlockProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBreakDoor(final EntityBreakDoorEvent e) {
        if (plugin.getBlockProtection(e.getBlock()).isPresent()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPistonRetract(final BlockPistonRetractEvent e) {
        for (final Block block : e.getBlocks()) {
            if (plugin.getBlockProtection(block).isPresent()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockPistonExtend(final BlockPistonExtendEvent e) {
        for (final Block block : e.getBlocks()) {
            if (plugin.getBlockProtection(block).isPresent()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockExplode(final BlockExplodeEvent e) {
        e.blockList().removeIf(block -> plugin.getBlockProtection(block).isPresent());
    }

    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent e) {
        e.blockList().removeIf(block -> plugin.getBlockProtection(block).isPresent());
    }

    @EventHandler
    public void onPlayerTakeLecternBook(final PlayerTakeLecternBookEvent e) {
        if (!plugin.canAccessBlock(e.getPlayer(), e.getLectern().getBlock(), Permission.WITHDRAW)) {
            e.setCancelled(true);
        }
    }

    public void onPlayerRecipeBookClick(final PlayerEvent e) {
        if (!(e instanceof Cancellable cancellable)) {
            return;
        }
        final Player player = e.getPlayer();
        final Location location = player.getOpenInventory().getTopInventory().getLocation();
        if (location != null && !plugin.canAccessBlock(player, location.getBlock(), Permission.DEPOSIT, Permission.WITHDRAW)) {
            cancellable.setCancelled(true);
        }
    }
}

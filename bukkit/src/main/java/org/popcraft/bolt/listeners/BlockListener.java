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
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.popcraft.bolt.BoltPlugin;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.util.Action;
import org.popcraft.bolt.util.BoltComponents;
import org.popcraft.bolt.util.BukkitAdapter;
import org.popcraft.bolt.util.Permission;
import org.popcraft.bolt.util.PlayerMeta;
import org.popcraft.bolt.util.Protections;
import org.popcraft.bolt.util.lang.Strings;
import org.popcraft.bolt.util.lang.Translation;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.popcraft.bolt.util.lang.Translator.translate;

@SuppressWarnings("ClassCanBeRecord")
public final class BlockListener implements Listener {
    private static final EnumSet<BlockFace> CARTESIAN_BLOCK_FACES = EnumSet.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
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
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        if (playerMeta.hasInteracted()) {
            e.setCancelled(true);
            return;
        }
        final Protection protection = plugin.findProtection(clicked).orElse(null);
        if (triggerActions(player, protection, clicked)) {
            playerMeta.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, playerMeta::clearInteraction);
            e.setCancelled(true);
        } else if (protection != null) {
            final boolean hasNotifyPermission = player.hasPermission("bolt.protection.notify");
            if (!plugin.canAccessProtection(player, protection, Permission.INTERACT)) {
                e.setCancelled(true);
                if (!hasNotifyPermission) {
                    BoltComponents.sendMessage(player, Translation.LOCKED, Template.of("type", Protections.displayType(protection)));
                }
            }
            if (hasNotifyPermission) {
                BoltComponents.sendMessage(player, Translation.PROTECTION_NOTIFY, Template.of("type", Protections.displayType(protection)), Template.of("owner", player.getUniqueId().equals(protection.getOwner()) ? translate(Translation.YOU) : BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN))));
            }
            if (e.getItem() != null) {
                final Material itemType = e.getItem().getType();
                if (Material.LECTERN.equals(clicked.getType()) && (Material.WRITABLE_BOOK.equals(itemType) || Material.WRITTEN_BOOK.equals(itemType)) && !plugin.canAccessProtection(player, protection, Permission.DEPOSIT)) {
                    e.setUseItemInHand(Event.Result.DENY);
                } else if ((Tag.SIGNS.isTagged(clicked.getType()) && (DYES.contains(itemType) || Material.GLOW_INK_SAC.equals(itemType)) && !plugin.canAccessProtection(player, protection, Permission.INTERACT))) {
                    e.setUseItemInHand(Event.Result.DENY);
                    e.setUseInteractedBlock(Event.Result.DENY);
                }
            }
            playerMeta.setInteracted();
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, playerMeta::clearInteraction);
        }
    }

    @SuppressWarnings("java:S6205")
    private boolean triggerActions(final Player player, final Protection protection, final Block block) {
        final PlayerMeta playerMeta = plugin.playerMeta(player);
        final Action action = playerMeta.triggerAction();
        if (action == null) {
            return false;
        }
        switch (action) {
            case LOCK -> {
                if (protection != null) {
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED_ALREADY, Template.of("type", Protections.displayType(protection)));
                } else {
                    plugin.getBolt().getStore().saveBlockProtection(BukkitAdapter.createPrivateBlockProtection(block, playerMeta.isLockNil() ? UUID.fromString("00000000-0000-0000-0000-000000000000") : player.getUniqueId()));
                    playerMeta.setLockNil(false);
                    BoltComponents.sendMessage(player, Translation.CLICK_LOCKED, Template.of("type", Protections.displayType(block)));
                }
            }
            case UNLOCK -> {
                if (protection != null) {
                    plugin.removeProtection(protection);
                    BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Protections.displayType(protection)));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Protections.displayType(block)));
                }
            }
            case INFO -> {
                if (protection != null) {
                    BoltComponents.sendMessage(player, Translation.INFO, Template.of("access", Strings.toTitleCase(protection.getType())), Template.of("owner", BukkitAdapter.playerName(protection.getOwner()).orElse(translate(Translation.UNKNOWN))));
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Protections.displayType(block)));
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
                        plugin.saveProtection(protection);
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED, Template.of("type", Protections.displayType(protection)));
                    } else {
                        BoltComponents.sendMessage(player, Translation.CLICK_EDITED_NO_PERMISSION);
                    }
                } else {
                    BoltComponents.sendMessage(player, Translation.CLICK_NOT_LOCKED, Template.of("type", Protections.displayType(block)));
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
                    final Optional<Protection> firstProtection = plugin.findProtection(firstBlock);
                    firstProtection.ifPresent(blockProtection -> {
                        if (plugin.canAccessProtection(player, blockProtection, Permission.DESTROY)) {
                            plugin.removeProtection(blockProtection);
                        } else {
                            e.setCancelled(true);
                        }
                    });
                    final Optional<Protection> secondProtection = plugin.findProtection(secondBlock);
                    secondProtection.ifPresent(blockProtection -> {
                        if (plugin.canAccessProtection(player, blockProtection, Permission.DESTROY)) {
                            plugin.removeProtection(blockProtection);
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
        final Optional<Protection> optionalProtection = plugin.findProtection(block);
        final Player player = e.getPlayer();
        if (optionalProtection.isPresent()) {
            final Protection protection = optionalProtection.get();
            if (plugin.canAccessProtection(player, protection, Permission.DESTROY)) {
                plugin.removeProtection(protection);
                BoltComponents.sendMessage(player, Translation.CLICK_UNLOCKED, Template.of("type", Protections.displayType(protection)));
            } else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSignChange(final SignChangeEvent e) {
        if (!plugin.canAccessBlock(e.getPlayer(), e.getBlock(), Permission.INTERACT)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(final StructureGrowEvent e) {
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
        for (final Block block : e.getBlocks()) {
            if (plugin.findProtection(block).isPresent()) {
                e.setCancelled(true);
                return;
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

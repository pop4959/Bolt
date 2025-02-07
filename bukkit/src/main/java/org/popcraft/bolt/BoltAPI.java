package org.popcraft.bolt;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.popcraft.bolt.event.Event;
import org.popcraft.bolt.protection.BlockProtection;
import org.popcraft.bolt.protection.EntityProtection;
import org.popcraft.bolt.protection.Protection;
import org.popcraft.bolt.source.PlayerSourceResolver;
import org.popcraft.bolt.source.SourceResolver;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Bolt API methods. It is meant to be used through the {@link org.bukkit.plugin.ServicesManager services manager}.
 * <p>
 * {@snippet lang=java :
 * BoltAPI bolt = Bukkit.getServer().getServicesManager().load(BoltAPI.class);
 * }
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public interface BoltAPI {
    /**
     * Checks whether the given block can be protected according to the {@code blocks} section of the config.
     */
    boolean isProtectable(final Block block);

    /**
     * Checks whether the given entity can be protected according to the {@code entities} section of the config.
     */
    boolean isProtectable(final Entity entity);

    /**
     * Checks whether the given block is currently protected in any way. This method will return true for blocks that
     * are not protected themselves but are in some way supporting another protected block or entity.
     *
     * @see #isProtectedExact(Block)
     */
    boolean isProtected(final Block block);

    /**
     * Checks whether the given entity is currently protected in any way. This method will return true for entities that
     * are not protected themselves but are in some way supporting another protected block or entity.
     *
     * @see #isProtectedExact(Entity)
     */
    boolean isProtected(final Entity entity);

    /**
     * Checks whether the given block is protected. This method ignores other protected blocks which are supported by
     * this block.
     *
     * @see #isProtected(Block)
     */
    boolean isProtectedExact(final Block block);

    /**
     * Checks whether the given entity is protected. This method ignores other protected entities which are supported by
     * this entity.
     *
     * @see #isProtected(Entity)
     */
    boolean isProtectedExact(final Entity entity);

    // TODO(rymiel): this method doesn't check if `type` is valid, and you can't check that yourself in the API currently.
    //   If you provide an invalid one, it's as if it had no protection type and allows nothing.
    /**
     * Creates a new block protection. The created protection is NOT saved to storage, you must call {@link #saveProtection(Protection)}
     * for this protection to exist in the world. This method does not check if the provided block can normally be protected, use
     * {@link #isProtectable(Block)} to check that.
     *
     * @param block block to be protected
     * @param owner owner of the protection. May be {@link org.popcraft.bolt.util.Profiles#NIL_UUID NIL_UUID} for a protection not owned by anyone in particular
     * @param type protection type. See the {@code protections} section in the config
     * @return protection object representing the newly created protection
     */
    BlockProtection createProtection(final Block block, final UUID owner, final String type);

    /**
     * Creates a new entity protection. The created protection is NOT saved to storage, you must call {@link #saveProtection(Protection)}
     * for this protection to exist in the world. This method does not check if the provided entity can normally be protected, use
     * {@link #isProtectable(Entity)} to check that.
     *
     * @param entity entity to be protected
     * @param owner owner of the protection. May be {@link org.popcraft.bolt.util.Profiles#NIL_UUID NIL_UUID} for a protection not owned by anyone in particular
     * @param type protection type. See the {@code protections} section in the config
     * @return protection object representing the newly created protection
     */
    EntityProtection createProtection(final Entity entity, final UUID owner, final String type);

    /**
     * Loads ALL block and entity protections in all worlds.
     */
    Collection<Protection> loadProtections();

    /**
     * Loads the protection associated with the given block. Does not consider supporting blocks, like {@link #isProtectedExact(Block)}.
     * @return protection object, or {@code null} if no protection exists for this block.
     * @see #findProtection(Block)
     */
    BlockProtection loadProtection(final Block block);

    /**
     * Loads the protection associated with the given entity. Does not consider supporting entities, like {@link #isProtectedExact(Entity)}.
     * @return protection object, or {@code null} if no protection exists for this entity.
     * @see #findProtection(Entity)
     */
    EntityProtection loadProtection(final Entity entity);

    /**
     * Persists the given protection object to storage and makes it exist in the world.
     */
    void saveProtection(final Protection protection);

    /**
     * Removes the given protection object from storage, making it no longer exist in the world.
     */
    void removeProtection(final Protection protection);

    /**
     * Loads the protection associated with the given block or another protected block or entity that this block supports, like {@link #isProtected(Block)}.
     * @return protection object, or {@code null} if no protection exists for this block.
     * @see #loadProtection(Block) 
     */
    Protection findProtection(final Block block);

    /**
     * Loads the protection associated with the given entity or another protected block or entity that this entity supports, like {@link #isProtected(Entity)}.
     * @return protection object, or {@code null} if no protection exists for this entity.
     * @see #findProtection(Entity)
     */
    Protection findProtection(final Entity entity);

    /**
     * Finds ALL protections contained within the bounding box. This includes block and entity protections.
     */
    Collection<Protection> findProtections(final World world, final BoundingBox boundingBox);

    /**
     * Checks whether the given player can access the given block with the given permissions.
     * Returns true if no protection exists at that block.
     * See {@link org.popcraft.bolt.util.Permission Permission} for permissions that exist.
     */
    boolean canAccess(final Block block, final Player player, final String... permissions);

    /**
     * Checks whether the given player can access the given entity with the given permissions.
     * Returns true if no protection exists for that entity.
     * See {@link org.popcraft.bolt.util.Permission Permission} for permissions that exist.
     */
    boolean canAccess(final Entity entity, final Player player, final String... permissions);

    /**
     * Checks whether the given player can access the given protection with the given permissions.
     * See {@link org.popcraft.bolt.util.Permission Permission} for permissions that exist.
     */
    boolean canAccess(final Protection protection, final Player player, final String... permissions);

    /**
     * Checks whether the player with the given UUID can access the given protection with the given permissions.
     * See {@link org.popcraft.bolt.util.Permission Permission} for permissions that exist.
     */
    boolean canAccess(final Protection protection, final UUID uuid, final String... permissions);

    /**
     * Checks whether the given source resolver can access the given protection with the given permissions.
     * See {@link org.popcraft.bolt.util.Permission Permission} for permissions that exist.
     */
    boolean canAccess(final Protection protection, final SourceResolver sourceResolver, final String... permissions);

    /**
     * Registers a source resolver for players. This source resolver is checked every time a player tries to access a
     * protection and is passed the source and the player's UUID for each source that could access the protection.
     *
     * @see org.popcraft.bolt.source.Source Source
     * @see SourceResolver
     */
    void registerPlayerSourceResolver(final PlayerSourceResolver playerSourceResolver);

    /**
     * Registers an event listener for the given event class.
     */
    <T extends Event> void registerListener(final Class<T> clazz, final Consumer<? super T> listener);
}

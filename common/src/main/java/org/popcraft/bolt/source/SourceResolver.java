package org.popcraft.bolt.source;

/**
 * A source resolver is something that can try to access a location. It is passed sources, and returns whether the given
 * source is applicable to this source resolver.
 * <p>
 * For example, a source resolver representing a player would return {@code true} when given a player source with the
 * player's UUID. Or a group source with a group that the player is part of.
 */
@FunctionalInterface
public interface SourceResolver {
    boolean resolve(Source source);
}

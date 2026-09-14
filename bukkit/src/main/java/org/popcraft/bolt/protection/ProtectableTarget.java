package org.popcraft.bolt.protection;

/**
 * Describes how a custom block can be protected. This is equivalent to an entry in the {@code blocks} section of the
 * config.
 *
 * @param key                   identifier used in permission nodes such as {@code bolt.protection.lock.<key>}. Should
 *                              be lowercase and prefixed to avoid collisions with vanilla names, for example
 *                              {@code myplugin_machine}
 * @param autoProtect           protection type to automatically protect with when placed, or {@code null} to not
 *                              automatically protect. See the {@code protections} section in the config
 * @param lockPermission        whether {@code bolt.protection.lock.<key>} is required to lock
 * @param autoProtectPermission whether {@code bolt.protection.autoprotect.<key>} is required to automatically protect
 */
public record ProtectableTarget(String key, String autoProtect, boolean lockPermission, boolean autoProtectPermission) {
}

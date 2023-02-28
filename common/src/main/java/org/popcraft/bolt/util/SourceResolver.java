package org.popcraft.bolt.util;

@FunctionalInterface
public interface SourceResolver {
    boolean resolve(Source source);
}

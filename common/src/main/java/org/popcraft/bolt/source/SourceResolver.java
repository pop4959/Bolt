package org.popcraft.bolt.source;

@FunctionalInterface
public interface SourceResolver {
    boolean resolve(Source source);
}

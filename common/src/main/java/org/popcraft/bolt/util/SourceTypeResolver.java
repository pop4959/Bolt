package org.popcraft.bolt.util;

public record SourceTypeResolver(Source source) implements SourceResolver {
    @Override
    public boolean resolve(Source source) {
        return this.source.getType().equals(source.getType());
    }
}

package org.popcraft.bolt.source;

public record SourceTypeResolver(Source source) implements SourceResolver {
    @Override
    public boolean resolve(Source source) {
        return this.source.getType().equals(source.getType());
    }
}

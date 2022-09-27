package org.popcraft.bolt.util.lang;

import org.popcraft.bolt.util.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.popcraft.bolt.util.lang.Translator.translate;

public class Strings {
    private Strings() {
    }

    public static String toTitleCase(final Object obj) {
        return toTitleCase(obj == null ? "null" : obj.toString());
    }

    public static String toTitleCase(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        final StringBuilder builder = new StringBuilder();
        final char[] chars = str.toLowerCase().replace("_", " ").toCharArray();
        builder.append(Character.toUpperCase(chars[0]));
        for (int i = 1; i < chars.length; ++i) {
            builder.append(Character.isWhitespace(chars[i - 1]) ? Character.toUpperCase(chars[i]) : chars[i]);
        }
        return builder.toString();
    }

    public static String access(final Map<String, String> accessMap) {
        if (accessMap == null || accessMap.isEmpty()) {
            return translate(Translation.EMPTY);
        }
        final List<String> access = new ArrayList<>();
        for (final String source : accessMap.keySet()) {
            access.add("%s (%s)".formatted(Source.identifier(source), toTitleCase(Source.type(source))));
        }
        return String.join(", ", access);
    }
}

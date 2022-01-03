package org.popcraft.bolt.util.lang;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public final class Translator {
    private static final Properties fallback = loadTranslation("en");
    private static Properties translations = loadTranslation("en");

    private Translator() {
    }

    public static String translate(final String key) {
        return Objects.requireNonNullElse(translations.getProperty(key), Objects.requireNonNullElse(fallback.getProperty(key), key));
    }

    private static void setTranslation(final String language) {
        translations = loadTranslation(language);
    }

    private static Properties loadTranslation(final String language) {
        final Properties properties = new Properties();
        final InputStream input = Translator.class.getClassLoader().getResourceAsStream("lang/" + language + ".properties");
        if (input != null) {
            try {
                properties.load(input);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }
}

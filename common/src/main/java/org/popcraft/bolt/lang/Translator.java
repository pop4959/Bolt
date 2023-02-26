package org.popcraft.bolt.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static void load(final Path directory, final String language) {
        if (language == null) {
            return;
        }
        if (directory != null) {
            final Path filePath = directory.resolve(language + ".properties");
            if (Files.exists(filePath)) {
                translations = loadTranslationFromFile(filePath);
                return;
            }
        }
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

    private static Properties loadTranslationFromFile(final Path path) {
        final Properties properties = new Properties();
        try (final BufferedReader input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}

package org.popcraft.bolt.util.lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
                translations = loadTranslationFromFile(filePath.toFile());
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

    private static Properties loadTranslationFromFile(final File file) {
        final Properties properties = new Properties();
        try (final InputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}

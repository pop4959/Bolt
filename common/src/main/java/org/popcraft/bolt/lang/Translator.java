package org.popcraft.bolt.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class Translator {
    private static final String TRANSLATION_FILE_FORMAT = "lang/%s.properties";
    private static final String DEFAULT_LOCALE_FORMAT = "%s_%s";
    private static final Properties fallback = loadTranslation("en");
    private static Properties translations = loadTranslation("en");
    private static String selected = "en";

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

    public static String selected() {
        return selected;
    }

    private static Properties loadTranslation(final String language) {
        final Properties properties = new Properties();
        final ClassLoader classLoader = Translator.class.getClassLoader();
        String newSelected = language;
        InputStream input = classLoader.getResourceAsStream(TRANSLATION_FILE_FORMAT.formatted(language));
        if (input == null) {
            newSelected = DEFAULT_LOCALE_FORMAT.formatted(language.toLowerCase(), language.toUpperCase());
            input = classLoader.getResourceAsStream(TRANSLATION_FILE_FORMAT.formatted(newSelected));
        }
        if (input != null) {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                properties.load(reader);
                selected = newSelected;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    private static Properties loadTranslationFromFile(final Path path) {
        final Properties properties = new Properties();
        try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
            selected = "custom";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}

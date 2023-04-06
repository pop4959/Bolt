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
    private static final Properties fallback = loadTranslation("en");
    private static Properties translation = loadTranslation("en");
    private static Properties customTranslation = loadTranslation("en");
    private static String selected = "en";

    private Translator() {
    }

    public static boolean isTranslatable(final String key) {
        return customTranslation.containsKey(key) || translation.containsKey(key) || fallback.containsKey(key);
    }

    public static String translate(final String key) {
        return Objects.requireNonNullElseGet(customTranslation.getProperty(key), () -> Objects.requireNonNullElseGet(translation.getProperty(key), () -> Objects.requireNonNullElse(fallback.getProperty(key), key)));
    }

    public static void load(final Path directory, final String language) {
        translation = loadTranslation(language);
        final Path filePath = directory.resolve(language + ".properties");
        customTranslation = loadTranslationFromFile(filePath);
        if (!customTranslation.isEmpty()) {
            selected = "custom";
        } else if (!translation.isEmpty()) {
            selected = language;
        } else {
            selected = "en";
        }
    }

    public static String selected() {
        return selected;
    }

    private static Properties loadTranslation(final String language) {
        final ClassLoader classLoader = Translator.class.getClassLoader();
        final Properties properties = new Properties();
        final String translationFile = TRANSLATION_FILE_FORMAT.formatted(language);
        try (final InputStream input = Objects.requireNonNullElseGet(classLoader.getResourceAsStream(translationFile), InputStream::nullInputStream);
             final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static Properties loadTranslationFromFile(final Path path) {
        final Properties properties = new Properties();
        if (!Files.exists(path)) {
            return properties;
        }
        try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}

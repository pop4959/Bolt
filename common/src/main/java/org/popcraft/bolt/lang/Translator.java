package org.popcraft.bolt.lang;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.stream.Stream;

public final class Translator {
    private static final String TRANSLATION_FILE_FORMAT = "lang/%s.properties";
    private static final Properties fallback = loadTranslation("en");
    private static Properties translation = loadTranslation("en");
    private static String selected = "en";
    private static boolean perPlayerLocale = true;
    private static final Map<Locale, Properties> languages = new HashMap<>();

    private Translator() {
    }

    public static boolean isTranslatable(final String key, final Locale locale) {
        if (!perPlayerLocale) {
            return translation.containsKey(key) || fallback.containsKey(key);
        }

        return tryGetProperty(key, locale) != null
            || tryGetProperty(key, new Locale(locale.getLanguage())) != null
            || translation.containsKey(key)
            || fallback.containsKey(key);
    }

    public static String translate(final String key, final Locale locale) {
        if (!perPlayerLocale) {
            return Objects.requireNonNullElseGet(translation.getProperty(key),
                () -> Objects.requireNonNullElse(fallback.getProperty(key), key));
        }

        return Objects.requireNonNullElseGet(tryGetProperty(key, locale),
            () -> Objects.requireNonNullElseGet(tryGetProperty(key, new Locale(locale.getLanguage())),
                () -> Objects.requireNonNullElseGet(translation.getProperty(key),
                    () -> Objects.requireNonNullElse(fallback.getProperty(key), key))));
    }

    private static String tryGetProperty(final String key, final Locale locale) {
        final Properties properties = languages.get(locale);
        if (properties == null) return null;

        return properties.getProperty(key);
    }

    public static String selected() {
        return selected;
    }

    public static void loadAllTranslations(final Path directory, final String preferredLanguage, final boolean perPlayerLocales) {
        final long startTimeNanos = System.nanoTime();

        // Load all the localization files bundled with the jar
        final ClassLoader classLoader = Translator.class.getClassLoader();
        try {
            // This is like using a bazooka to kill a fly (where the bazooka is "FileSystems" and the fly is
            // "just loading all the translation files"
            final URI uri = Objects.requireNonNull(classLoader.getResource("lang/")).toURI();
            try (final FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
                 Stream<Path> walk = Files.walk(fileSystem.getPath("lang/"), 1)) {
                for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
                    Path path = it.next();
                    if (!path.toString().endsWith(".properties")) continue;

                    final String localeName = path.getFileName().toString().split("\\.")[0].replace('_', '-');
                    final Locale locale = Locale.forLanguageTag(localeName);
                    final Properties properties = loadTranslation(locale.toString());
                    languages.put(locale, properties);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        // Load user-defined localization files. This is done after, so it overrides any built-in translations.
        File[] files = directory.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".properties"));
        for (File file : Objects.requireNonNull(files)) {
            final String localeName = file.getName().split("\\.")[0].replace('_', '-');
            final Locale locale = Locale.forLanguageTag(localeName);
            final Properties properties = loadTranslationFromFile(file.toPath());
            languages.put(locale, properties);
        }

        // Load the preferred fallback language
        translation = languages.getOrDefault(Locale.forLanguageTag(preferredLanguage.replace('_', '-')), fallback);

        // This is no longer really that useful, but keep it around
        selected = preferredLanguage;
        perPlayerLocale = perPlayerLocales;

        final long timeNanos = System.nanoTime() - startTimeNanos;
        final double timeMillis = timeNanos / 1e6d;
        LogManager.getLogManager().getLogger("").info(() -> "Loaded %d localization files in %.3f ms".formatted(languages.size(), timeMillis));
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

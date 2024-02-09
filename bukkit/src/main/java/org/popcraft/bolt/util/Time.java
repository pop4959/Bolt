package org.popcraft.bolt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.popcraft.bolt.lang.Translation;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import static org.popcraft.bolt.util.BoltComponents.getLocaleOf;
import static org.popcraft.bolt.util.BoltComponents.resolveTranslation;

public class Time {
    private static final int DAYS_IN_YEAR = 365;
    private static final int DAYS_IN_MONTH = 30;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG);

    private Time() {
    }

    public static Component relativeTimestamp(long unixMs, CommandSender sender) {
        final long now = System.currentTimeMillis();

        final String timestamp = FORMATTER.withLocale(getLocaleOf(sender)).format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(unixMs), UTC));
        final Duration duration = Duration.of(now - unixMs, ChronoUnit.MILLIS);
        final Component ago = agoComponent(duration, sender);

        return resolveTranslation(Translation.TIME_AGO, sender, Placeholder.component(Translation.Placeholder.TIME, ago)).hoverEvent(Component.text(timestamp));
    }

    private static Component agoComponent(Duration duration, CommandSender sender) {
        final ArrayList<Component> parts = new ArrayList<>(5);

        long days = duration.toDays();
        final int hours = duration.toHoursPart();
        final int minutes = duration.toMinutesPart();
        final int seconds = duration.toSecondsPart();
        if (days >= DAYS_IN_YEAR) {
            final long years = Math.floorDiv(days, DAYS_IN_YEAR);
            days -= (years * DAYS_IN_YEAR);
            parts.add(resolveTranslation(Translation.TIME_YEARS, sender, Placeholder.component(Translation.Placeholder.NUMBER, Component.text(years))));
        }

        if (days >= DAYS_IN_MONTH) {
            final long months = Math.floorDiv(days, DAYS_IN_MONTH);
            days -= (months * DAYS_IN_MONTH);
            parts.add(resolveTranslation(Translation.TIME_MONTHS, sender, Placeholder.component(Translation.Placeholder.NUMBER, Component.text(months))));
        }

        if (days != 0) {
            parts.add(resolveTranslation(Translation.TIME_DAYS, sender, Placeholder.component(Translation.Placeholder.NUMBER, Component.text(days))));
        }

        if (hours != 0) {
            parts.add(resolveTranslation(Translation.TIME_HOURS, sender, Placeholder.component(Translation.Placeholder.NUMBER, Component.text(hours))));
        }

        if (minutes != 0) {
            parts.add(resolveTranslation(Translation.TIME_MINUTES, sender, Placeholder.component(Translation.Placeholder.NUMBER, Component.text(minutes))));
        }

        if (seconds != 0) {
            parts.add(resolveTranslation(Translation.TIME_SECONDS, sender, Placeholder.component(Translation.Placeholder.NUMBER, Component.text(seconds))));
        } else if (parts.isEmpty()) {
            // avoid an empty string, say "0s" instead
            parts.add(resolveTranslation(Translation.TIME_SECONDS, sender, Placeholder.component(Translation.Placeholder.NUMBER, Component.text(0))));
        }

        return Component.join(JoinConfiguration.spaces(), parts);
    }
}

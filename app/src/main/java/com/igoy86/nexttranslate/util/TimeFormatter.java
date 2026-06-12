package com.igoy86.nexttranslate.util;

import android.text.format.DateUtils;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for formatting timestamps in a consistent English format
 * regardless of the device's system locale.
 *
 * <p>Format rules:</p>
 * <ul>
 *     <li>Within 24 hours → relative time, e.g. "2 hours ago", "5 minutes ago"</li>
 *     <li>Older than 24 hours → absolute date, e.g. "23 May 2026"</li>
 * </ul>
 */
public final class TimeFormatter {

    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    /** Private constructor — static utility class, not instantiable. */
    private TimeFormatter() {}

    /**
     * Formats a timestamp as an English time string.
     *
     * @param timestampMs the timestamp in milliseconds (e.g. from {@link System#currentTimeMillis()})
     * @return a human-readable English time string
     */
    @NonNull
    public static String format(long timestampMs) {
        final long now  = System.currentTimeMillis();
        final long diff = now - timestampMs;

        if (diff < DAY_MS) {
            // Within 24 hours — relative string (e.g. "2 hours ago")
            return DateUtils.getRelativeTimeSpanString(
                    timestampMs,
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString();
        } else {
            // Older than 24 hours — absolute date in English (e.g. "23 May 2026")
            return new SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
                    .format(new Date(timestampMs));
        }
    }
}

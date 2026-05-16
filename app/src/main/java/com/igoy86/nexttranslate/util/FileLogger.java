package com.igoy86.nexttranslate.util;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A file-based logger utility that writes log entries to a text file
 * in the device's public Downloads folder.
 *
 * <p>This logger is designed for debugging on mobile devices (e.g. AndroidIDE)
 * where a PC-based debugger is not available. Log output is written to:</p>
 * <pre>
 *     /storage/emulated/0/Download/nexttranslate-log.txt
 * </pre>
 *
 * <p>Supports Android API 29–35:</p>
 * <ul>
 *     <li>API 29      : {@link Environment#getExternalStoragePublicDirectory} with legacy flag</li>
 *     <li>API 30–35   : {@link MediaStore.Downloads} via {@link android.content.ContentResolver}</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 *     FileLogger.init(context);
 *     FileLogger.d("MainActivity", "App started");
 *     FileLogger.e("TranslateRepo", "Translation failed", throwable);
 *     FileLogger.clear(context);
 * </pre>
 */
public class FileLogger {

    /** Tag used for internal logcat output from this class. */
    private static final String TAG = "FileLogger";

    /** The name of the log file written to the Downloads folder. */
    private static final String LOG_FILE_NAME = "nexttranslate-log.txt";

    /** Date-time format used for each log entry timestamp. */
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /** Log level label for debug messages. */
    private static final String LEVEL_DEBUG = "DEBUG";

    /** Log level label for info messages. */
    private static final String LEVEL_INFO = "INFO";

    /** Log level label for warning messages. */
    private static final String LEVEL_WARN = "WARN";

    /** Log level label for error messages. */
    private static final String LEVEL_ERROR = "ERROR";

    /** Application context used for MediaStore access on API 30+. */
    private static Context appContext;

    /**
     * Private constructor to prevent instantiation.
     * All methods in this class are static utility methods.
     */
    private FileLogger() {
        throw new UnsupportedOperationException("FileLogger is a utility class and cannot be instantiated.");
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    /**
     * Initializes the {@link FileLogger} with the application context.
     *
     * <p>Must be called once before any logging methods are used,
     * typically in {@link android.app.Application#onCreate()}.</p>
     *
     * @param context the application {@link Context}
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
        Log.d(TAG, "FileLogger initialized. Logs will be written to: " + LOG_FILE_NAME);
    }

    // -------------------------------------------------------------------------
    // Public logging methods
    // -------------------------------------------------------------------------

    /**
     * Writes a DEBUG level log entry to the log file.
     *
     * @param tag     the source tag identifying the caller (e.g. class name)
     * @param message the debug message to log
     */
    public static void d(String tag, String message) {
        writeLog(LEVEL_DEBUG, tag, message, null);
    }

    /**
     * Writes an INFO level log entry to the log file.
     *
     * @param tag     the source tag identifying the caller
     * @param message the informational message to log
     */
    public static void i(String tag, String message) {
        writeLog(LEVEL_INFO, tag, message, null);
    }

    /**
     * Writes a WARNING level log entry to the log file.
     *
     * @param tag     the source tag identifying the caller
     * @param message the warning message to log
     */
    public static void w(String tag, String message) {
        writeLog(LEVEL_WARN, tag, message, null);
    }

    /**
     * Writes an ERROR level log entry to the log file without a throwable.
     *
     * @param tag     the source tag identifying the caller
     * @param message the error message to log
     */
    public static void e(String tag, String message) {
        writeLog(LEVEL_ERROR, tag, message, null);
    }

    /**
     * Writes an ERROR level log entry to the log file including a full
     * stack trace from the given {@link Throwable}.
     *
     * @param tag       the source tag identifying the caller
     * @param message   the error message to log
     * @param throwable the exception whose stack trace will be appended
     */
    public static void e(String tag, String message, Throwable throwable) {
        writeLog(LEVEL_ERROR, tag, message, throwable);
    }

    /**
     * Deletes the existing log file from the Downloads folder.
     *
     * <p>Call this method to clear accumulated logs, for example when
     * the user requests a log reset from the Settings screen.</p>
     */
    public static void clear() {
        if (appContext == null) {
            Log.e(TAG, "FileLogger not initialized. Call FileLogger.init(context) first.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clearViaMediaStore();
        } else {
            clearViaLegacyStorage();
        }
    }

    // -------------------------------------------------------------------------
    // Private write methods
    // -------------------------------------------------------------------------

    /**
     * Formats and appends a log entry to the log file.
     *
     * <p>Routes to {@link #writeViaMediaStore} on API 30+ or
     * {@link #writeViaLegacyStorage} on API 29.</p>
     *
     * @param level     the log level label (DEBUG, INFO, WARN, ERROR)
     * @param tag       the source tag
     * @param message   the log message
     * @param throwable optional throwable for stack trace, may be {@code null}
     */
    private static void writeLog(String level, String tag, String message, Throwable throwable) {
        if (appContext == null) {
            Log.e(TAG, "FileLogger not initialized. Call FileLogger.init(context) first.");
            return;
        }

        final String formattedEntry = formatLogEntry(level, tag, message, throwable);

        // Also mirror output to logcat for convenience
        Log.d(TAG, formattedEntry);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(formattedEntry);
        } else {
            writeViaLegacyStorage(formattedEntry);
        }
    }

    /**
     * Builds a formatted log entry string with timestamp, level, tag, message,
     * and optional stack trace.
     *
     * <p>Format:</p>
     * <pre>
     *     [2026-05-13 10:30:00.000] [ERROR] [TranslateRepo] Translation failed
     *     java.lang.Exception: ...
     *         at com.igoy86...
     * </pre>
     *
     * @param level     the log level label
     * @param tag       the source tag
     * @param message   the log message
     * @param throwable optional throwable, may be {@code null}
     * @return the fully formatted log entry string
     */
    private static String formatLogEntry(String level, String tag, String message, Throwable throwable) {
        final String timestamp = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault())
                .format(new Date());

        final StringBuilder entry = new StringBuilder();
        entry.append("[").append(timestamp).append("] ");
        entry.append("[").append(level).append("] ");
        entry.append("[").append(tag).append("] ");
        entry.append(message);
        entry.append("\n");

        if (throwable != null) {
            final StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            entry.append(stringWriter.toString());
            entry.append("\n");
        }

        return entry.toString();
    }

    // -------------------------------------------------------------------------
    // API 30+ : MediaStore approach
    // -------------------------------------------------------------------------

    /**
     * Appends a log entry to the log file using {@link MediaStore.Downloads}
     * for API 30 and above.
     *
     * <p>If the log file does not yet exist in MediaStore, a new entry is
     * inserted. Subsequent writes append to the existing file.</p>
     *
     * @param logEntry the formatted log entry string to append
     */
    private static void writeViaMediaStore(String logEntry) {
        try {
            // Query for existing log file in Downloads
            Uri existingUri = queryExistingLogFile();

            if (existingUri != null) {
                // Append to existing file
                appendToMediaStoreFile(existingUri, logEntry);
            } else {
                // Create new log file in Downloads
                createNewMediaStoreFile(logEntry);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to write log via MediaStore: " + ex.getMessage());
        }
    }

    /**
     * Queries {@link MediaStore.Downloads} to find an existing log file
     * matching {@link #LOG_FILE_NAME}.
     *
     * @return the {@link Uri} of the existing log file, or {@code null} if not found
     */
    private static Uri queryExistingLogFile() {
        final String[] projection = { MediaStore.Downloads._ID };
        final String selection = MediaStore.Downloads.DISPLAY_NAME + " = ?";
        final String[] selectionArgs = { LOG_FILE_NAME };

        try (android.database.Cursor cursor = appContext.getContentResolver().query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID));
                return Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id));
            }
        }
        return null;
    }

    /**
     * Appends the given log entry to an existing MediaStore file identified by URI.
     *
     * @param fileUri  the {@link Uri} of the existing log file
     * @param logEntry the log entry string to append
     * @throws IOException if the output stream cannot be opened or written
     */
    private static void appendToMediaStoreFile(Uri fileUri, String logEntry) throws IOException {
        try (OutputStream outputStream = appContext.getContentResolver().openOutputStream(fileUri, "wa");
             OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
            writer.append(logEntry);
            writer.flush();
        }
    }

    /**
     * Creates a new log file in {@link MediaStore.Downloads} and writes
     * the first log entry to it.
     *
     * @param logEntry the initial log entry string to write
     * @throws IOException if the file cannot be created or written
     */
    private static void createNewMediaStoreFile(String logEntry) throws IOException {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, LOG_FILE_NAME);
        contentValues.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        final Uri fileUri = appContext.getContentResolver().insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
        );

        if (fileUri == null) {
            throw new IOException("Failed to create log file via MediaStore.");
        }

        try (OutputStream outputStream = appContext.getContentResolver().openOutputStream(fileUri);
             OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
            writer.write(logEntry);
            writer.flush();
        }
    }

    /**
     * Deletes the log file from {@link MediaStore.Downloads} on API 30+.
     */
    private static void clearViaMediaStore() {
        try {
            final Uri existingUri = queryExistingLogFile();
            if (existingUri != null) {
                appContext.getContentResolver().delete(existingUri, null, null);
                Log.d(TAG, "Log file cleared via MediaStore.");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to clear log file via MediaStore: " + ex.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // API 29 : Legacy storage approach
    // -------------------------------------------------------------------------

    /**
     * Appends a log entry to the log file using legacy external storage
     * for API 29 only.
     *
     * @param logEntry the formatted log entry string to append
     */
    private static void writeViaLegacyStorage(String logEntry) {
        try {
            final File downloadDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
            );

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            final File logFile = new File(downloadDir, LOG_FILE_NAME);

            // append = true
            try (FileWriter fileWriter = new FileWriter(logFile, true)) {
                fileWriter.append(logEntry);
                fileWriter.flush();
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to write log via legacy storage: " + ex.getMessage());
        }
    }

    /**
     * Deletes the log file from legacy external storage on API 29.
     */
    private static void clearViaLegacyStorage() {
        final File downloadDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
        );
        final File logFile = new File(downloadDir, LOG_FILE_NAME);

        if (logFile.exists()) {
            if (logFile.delete()) {
                Log.d(TAG, "Log file cleared via legacy storage.");
            } else {
                Log.e(TAG, "Failed to delete log file via legacy storage.");
            }
        }
    }
}
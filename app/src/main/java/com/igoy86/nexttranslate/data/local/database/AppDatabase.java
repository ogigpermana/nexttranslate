package com.igoy86.nexttranslate.data.local.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.igoy86.nexttranslate.data.local.dao.FavoriteDao;
import com.igoy86.nexttranslate.data.local.dao.HistoryDao;
import com.igoy86.nexttranslate.data.local.entity.FavoriteEntity;
import com.igoy86.nexttranslate.data.local.entity.HistoryEntity;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * The main Room database for the NextTranslate application.
 *
 * <p>Serves as the single source of truth for all locally persisted data,
 * including translation history and favorite translations.</p>
 *
 * <p>This class is a singleton — only one instance is created for the
 * entire application lifecycle, shared across all repositories via
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 *
 * <p>Database configuration:</p>
 * <ul>
 *     <li>Name    : {@code nexttranslate.db}</li>
 *     <li>Version : {@code 1}</li>
 *     <li>Tables  : {@code history}, {@code favorites}</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 *     AppDatabase db = AppDatabase.getInstance(context);
 *     HistoryDao historyDao = db.historyDao();
 *     FavoriteDao favoriteDao = db.favoriteDao();
 * </pre>
 *
 * <p>To migrate the database in future versions, add a
 * {@link androidx.room.migration.Migration} object to
 * {@link Room.Builder#addMigrations(androidx.room.migration.Migration...)}
 * before releasing an update.</p>
 */
@Database(
        entities = {
                HistoryEntity.class,
                FavoriteEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    /** Tag used for logging events originating from this class. */
    private static final String TAG = "AppDatabase";

    /** The filename for the Room database stored on the device. */
    private static final String DATABASE_NAME = "nexttranslate.db";

    /** Singleton instance of the database. */
    private static volatile AppDatabase instance;

    // -------------------------------------------------------------------------
    // Abstract DAO accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link HistoryDao} for accessing the {@code history} table.
     *
     * @return the Room-generated {@link HistoryDao} implementation
     */
    public abstract HistoryDao historyDao();

    /**
     * Returns the {@link FavoriteDao} for accessing the {@code favorites} table.
     *
     * @return the Room-generated {@link FavoriteDao} implementation
     */
    public abstract FavoriteDao favoriteDao();

    // -------------------------------------------------------------------------
    // Singleton accessor
    // -------------------------------------------------------------------------

    /**
     * Returns the singleton {@link AppDatabase} instance.
     *
     * <p>Creates the database on the first call using double-checked locking
     * to ensure thread-safe lazy initialization. Subsequent calls return the
     * cached instance.</p>
     *
     * @param context the application {@link Context} used to build the database;
     *                must not be null
     * @return the singleton {@link AppDatabase} instance
     */
    public static AppDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = buildDatabase(context.getApplicationContext());
                    FileLogger.d(TAG, "AppDatabase instance created: " + DATABASE_NAME);
                }
            }
        }
        return instance;
    }

    // -------------------------------------------------------------------------
    // Private builder
    // -------------------------------------------------------------------------

    /**
     * Builds and configures the {@link AppDatabase} instance.
     *
     * <p>Uses {@link Room#databaseBuilder} with a
     * {@link RoomDatabase.Callback} to log when the database is first
     * created or opened.</p>
     *
     * @param context the application {@link Context}
     * @return the fully configured {@link AppDatabase} instance
     */
    @NonNull
    private static AppDatabase buildDatabase(@NonNull Context context) {
        return Room.databaseBuilder(
                        context,
                        AppDatabase.class,
                        DATABASE_NAME
                )
                .addCallback(buildDatabaseCallback())
                .build();
    }

    /**
     * Builds a {@link RoomDatabase.Callback} that logs database lifecycle events.
     *
     * <p>Logs when the database is first created (initial install) and when
     * it is opened on subsequent app launches. Useful for debugging database
     * initialization issues on mobile without a PC debugger.</p>
     *
     * @return a {@link RoomDatabase.Callback} instance for lifecycle logging
     */
    @NonNull
    private static RoomDatabase.Callback buildDatabaseCallback() {
        return new RoomDatabase.Callback() {

            /**
             * Called the first time the database is created.
             * This is triggered only on a fresh install or after the database is deleted.
             *
             * @param db the newly created {@link SupportSQLiteDatabase}
             */
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
                FileLogger.i(TAG, "Database created for the first time: " + DATABASE_NAME);
            }

            /**
             * Called every time the database is opened, including on app launch.
             *
             * @param db the opened {@link SupportSQLiteDatabase}
             */
            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
                FileLogger.d(TAG, "Database opened: " + DATABASE_NAME);
            }
        };
    }
}
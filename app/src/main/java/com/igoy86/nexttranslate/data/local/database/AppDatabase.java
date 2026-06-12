package com.igoy86.nexttranslate.data.local.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.igoy86.nexttranslate.data.local.dao.CollectionDao;
import com.igoy86.nexttranslate.data.local.dao.CollectionWordDao;
import com.igoy86.nexttranslate.data.local.dao.FavoriteDao;
import com.igoy86.nexttranslate.data.local.dao.HistoryDao;
import com.igoy86.nexttranslate.data.local.entity.CollectionEntity;
import com.igoy86.nexttranslate.data.local.entity.CollectionWordEntity;
import com.igoy86.nexttranslate.data.local.entity.FavoriteEntity;
import com.igoy86.nexttranslate.data.local.entity.HistoryEntity;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * The main Room database for the NextTranslate application.
 *
 * <p>Serves as the single source of truth for all locally persisted data,
 * including translation history, favorite translations, user collections,
 * and words saved inside collections.</p>
 *
 * <p>This class is a singleton — only one instance is created for the
 * entire application lifecycle, shared across all repositories via
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 *
 * <p>Database configuration:</p>
 * <ul>
 *     <li>Name    : {@code nexttranslate.db}</li>
 *     <li>Version : {@code 3}</li>
 *     <li>Tables  : {@code history}, {@code favorites}, {@code collections},
 *                   {@code collection_words}</li>
 * </ul>
 *
 * <p>Migration history:</p>
 * <ul>
 *     <li>Version 1 → 2: added {@code collections} table</li>
 *     <li>Version 2 → 3: added {@code collection_words} table with a
 *         CASCADE foreign key referencing {@code collections(id)}</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>
 *     AppDatabase db = AppDatabase.getInstance(context);
 *     HistoryDao historyDao = db.historyDao();
 *     CollectionWordDao wordDao = db.collectionWordDao();
 * </pre>
 */
@Database(
        entities = {
                HistoryEntity.class,
                FavoriteEntity.class,
                CollectionEntity.class,
                CollectionWordEntity.class
        },
        version = 3,
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
    // Migration: version 2 → 3
    // -------------------------------------------------------------------------

    /**
     * Room migration from database version 2 to version 3.
     *
     * <p>Creates the {@code collection_words} table with a CASCADE foreign key
     * on {@code collection_id} referencing the {@code collections} table.
     * An index on {@code collection_id} is also created to speed up queries
     * that filter words by collection.</p>
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `collection_words` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`collection_id` INTEGER NOT NULL, " +
                    "`word` TEXT NOT NULL, " +
                    "`definition` TEXT NOT NULL, " +
                    "`added_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`collection_id`) REFERENCES `collections`(`id`) " +
                    "ON DELETE CASCADE)"
            );
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_collection_words_collection_id` " +
                    "ON `collection_words` (`collection_id`)"
            );
            FileLogger.i(TAG, "Migration 2→3 applied: collection_words table created.");
        }
    };

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

    /**
     * Returns the {@link CollectionDao} for accessing the {@code collections} table.
     *
     * @return the Room-generated {@link CollectionDao} implementation
     */
    public abstract CollectionDao collectionDao();

    /**
     * Returns the {@link CollectionWordDao} for accessing the {@code collection_words} table.
     *
     * @return the Room-generated {@link CollectionWordDao} implementation
     */
    public abstract CollectionWordDao collectionWordDao();

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
     * <p>Registers {@link #MIGRATION_2_3} to handle existing installs that
     * are upgrading from version 2. Uses a {@link RoomDatabase.Callback}
     * to log database lifecycle events.</p>
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
                .addMigrations(MIGRATION_2_3)
                .addCallback(buildDatabaseCallback())
                .build();
    }

    /**
     * Builds a {@link RoomDatabase.Callback} that logs database lifecycle events.
     *
     * <p>Logs when the database is first created (initial install) and when
     * it is opened on subsequent app launches. Useful for debugging database
     * initialization issues on a mobile-only development environment.</p>
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

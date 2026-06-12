package com.igoy86.nexttranslate.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.igoy86.nexttranslate.data.local.entity.HistoryEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for the {@code history} table.
 *
 * <p>Provides the SQL operations required to manage translation history
 * records in the Room database. All methods in this interface are
 * implemented automatically by Room at compile time.</p>
 *
 * <p>This interface belongs to the data layer. The domain layer never
 * interacts with this DAO directly — it communicates via the
 * {@link com.igoy86.nexttranslate.domain.repository.HistoryRepository}
 * interface implemented by
 * {@link com.igoy86.nexttranslate.data.repository.HistoryRepositoryImpl}.</p>
 *
 * <p>Threading rules:</p>
 * <ul>
 *     <li>Write operations ({@link #insert}, {@link #insertAndGetId},
 *         {@link #updateHistory}, {@link #deleteById}, {@link #clearAll})
 *         must be called from a background thread.</li>
 *     <li>Read operations return {@link LiveData}, which Room updates
 *         automatically on the main thread.</li>
 * </ul>
 */
@Dao
public interface HistoryDao {

    /**
     * Inserts a new {@link HistoryEntity} into the {@code history} table.
     *
     * <p>If a record with the same primary key already exists, it will be
     * replaced ({@link OnConflictStrategy#REPLACE}).</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param entity the history entity to insert; must not be null
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntity entity);

    /**
     * Inserts a new {@link HistoryEntity} into the {@code history} table
     * and returns the auto-generated row ID assigned by Room.
     *
     * <p>Used by the upsert pattern in {@code TranslateViewModel} to track
     * the current session's history entry. The returned ID is stored in
     * {@code lastHistoryId} and reused for subsequent {@link #updateHistory}
     * calls within the same translate session.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param entity the history entity to insert; must not be null
     * @return the auto-generated primary key of the newly inserted row
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertAndGetId(HistoryEntity entity);

    /**
     * Updates the text content and timestamp of an existing history record
     * identified by its primary key.
     *
     * <p>Only {@code sourceText}, {@code translatedText},
     * {@code sourceLanguageCode}, and {@code timestamp} are updated.
     * {@code targetLanguageCode} is intentionally excluded — a language
     * change always starts a new session (new INSERT), so the target
     * language of an existing entry should never change in-place.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param id                 the primary key of the entry to update
     * @param sourceText         the updated original source text
     * @param translatedText     the updated translated text
     * @param sourceLanguageCode the updated source language BCP-47 code
     * @param timestamp          the updated Unix timestamp in milliseconds
     */
    @Query("UPDATE history SET " +
       "source_text = :sourceText, " +
       "translated_text = :translatedText, " +
       "source_language = :sourceLanguageCode, " +
       "timestamp = :timestamp " +
       "WHERE id = :id")
    void updateHistory(long id,
                       String sourceText,
                       String translatedText,
                       String sourceLanguageCode,
                       long timestamp);

    /**
     * Returns a {@link LiveData} list of all history records in the
     * {@code history} table, ordered from newest to oldest by timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying table changes. Observed on the main thread.</p>
     *
     * @return a {@link LiveData} emitting the full list of {@link HistoryEntity} objects
     */
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    LiveData<List<HistoryEntity>> getAllHistory();

    /**
     * Deletes a single history record identified by its primary key.
     *
     * <p>If no record with the given ID exists, this operation is a no-op.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @param id the primary key of the history record to delete
     */
    @Query("DELETE FROM history WHERE id = :id")
    void deleteById(long id);

    /**
     * Deletes all records from the {@code history} table.
     *
     * <p>This operation is irreversible. Must be called from a
     * background thread.</p>
     */
    @Query("DELETE FROM history")
    void clearAll();

    /**
     * Returns the total number of records in the {@code history} table.
     *
     * <p>Useful for checking whether the history is empty before showing
     * an empty state UI.</p>
     *
     * <p>Must be called from a background thread.</p>
     *
     * @return the total count of history records
     */
    @Query("SELECT COUNT(*) FROM history")
    int getHistoryCount();
}

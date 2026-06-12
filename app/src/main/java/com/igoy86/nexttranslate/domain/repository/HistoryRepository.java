package com.igoy86.nexttranslate.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.HistoryItem;

import java.util.List;

/**
 * Repository interface defining the contract for translation history
 * persistence operations.
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * Room or any Android framework classes directly. The actual implementation
 * resides in the data layer ({@code HistoryRepositoryImpl}).</p>
 *
 * <p>All write operations (insert, update, delete) are performed on a
 * background thread via {@link com.igoy86.nexttranslate.util.AppExecutors}.
 * Read operations return {@link LiveData} that Room updates automatically
 * whenever the underlying data changes.</p>
 */
public interface HistoryRepository {

    /**
     * Inserts a new {@link HistoryItem} into the database (fire-and-forget).
     *
     * <p>Executed on a background disk I/O thread. If an entry with the
     * same primary key already exists, it will be replaced.</p>
     *
     * @param item the {@link HistoryItem} to insert; must not be null
     */
    void addHistory(@NonNull HistoryItem item);

    /**
     * Inserts a new {@link HistoryItem} and delivers the auto-generated
     * row ID via {@link InsertCallback} on the disk I/O thread.
     *
     * <p>Used by the upsert pattern in {@code TranslateViewModel} to track
     * the current translate session. The caller stores the returned ID and
     * uses it for subsequent {@link #updateHistory} calls within the same
     * session.</p>
     *
     * @param item     the {@link HistoryItem} to insert; must not be null
     * @param callback receives the generated ID; invoked on background thread
     */
    void insertAndGetId(@NonNull HistoryItem item, @NonNull InsertCallback callback);

    /**
     * Updates an existing history entry identified by its database ID.
     *
     * <p>Only text content and timestamp are updated. Target language code
     * is immutable per entry — a language change always resets the session
     * and triggers a fresh INSERT via {@link #addHistory}.</p>
     *
     * <p>Executed on a background disk I/O thread.</p>
     *
     * @param id                 the database ID of the entry to update
     * @param sourceText         the updated original source text; must not be null
     * @param translatedText     the updated translated result; must not be null
     * @param sourceLanguageCode the updated source language BCP-47 code; must not be null
     * @param timestamp          the updated Unix timestamp in milliseconds
     */
    void updateHistory(long id,
                       @NonNull String sourceText,
                       @NonNull String translatedText,
                       @NonNull String sourceLanguageCode,
                       long timestamp);

    /**
     * Returns a {@link LiveData} list of all translation history entries,
     * ordered from newest to oldest by timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying data changes.</p>
     *
     * @return a {@link LiveData} emitting the full list of {@link HistoryItem} objects
     */
    LiveData<List<HistoryItem>> getAllHistory();

    /**
     * Deletes a single {@link HistoryItem} from the database by its ID.
     *
     * <p>Executed on a background disk I/O thread. If no entry with the
     * given ID exists, this operation is a no-op.</p>
     *
     * @param id the unique database ID of the history entry to delete
     */
    void deleteHistory(long id);

    /**
     * Deletes all translation history entries from the database.
     *
     * <p>Executed on a background disk I/O thread. This operation is
     * irreversible.</p>
     */
    void clearAllHistory();

    /**
     * Callback interface used to receive the auto-generated row ID
     * after a successful {@link #insertAndGetId} operation.
     *
     * <p>The callback is invoked on the disk I/O background thread.
     * Callers that need to update UI or ViewModel state must post back
     * to the main thread via a Handler or
     * {@link com.igoy86.nexttranslate.util.AppExecutors#mainThread()}.</p>
     */
    interface InsertCallback {

        /**
         * Called when the insert operation completes successfully.
         *
         * @param generatedId the auto-generated primary key of the new row;
         *                    always &gt; 0 on success
         */
        void onInserted(long generatedId);
    }
}

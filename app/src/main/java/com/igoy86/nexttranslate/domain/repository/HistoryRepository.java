package com.igoy86.nexttranslate.domain.repository;

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
 * <p>All write operations (insert, delete) are performed on a background
 * thread via {@link com.igoy86.nexttranslate.util.AppExecutors}.
 * Read operations return {@link LiveData} that Room updates automatically
 * whenever the underlying data changes.</p>
 */
public interface HistoryRepository {

    /**
     * Inserts a new {@link HistoryItem} into the database.
     *
     * <p>Executed on a background disk I/O thread. If an entry with the
     * same primary key already exists, it will be replaced.</p>
     *
     * @param item the {@link HistoryItem} to insert; must not be null
     */
    void addHistory(HistoryItem item);

    /**
     * Returns a {@link LiveData} list of all translation history entries,
     * ordered from newest to oldest by timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying history table changes.</p>
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
}
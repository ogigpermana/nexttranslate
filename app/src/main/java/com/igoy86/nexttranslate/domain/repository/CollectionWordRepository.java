package com.igoy86.nexttranslate.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.CollectionWordItem;

import java.util.List;

/**
 * Repository interface defining the contract for collection word
 * persistence operations.
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * Room or any Android framework classes directly. The actual implementation
 * resides in the data layer
 * ({@link com.igoy86.nexttranslate.data.repository.CollectionWordRepositoryImpl}).</p>
 *
 * <p>All write operations are performed on a background thread via
 * {@link com.igoy86.nexttranslate.util.AppExecutors#diskIO()}.
 * Read operations return {@link LiveData} that Room updates automatically
 * whenever the underlying data changes.</p>
 */
public interface CollectionWordRepository {

    /**
     * Inserts a new {@link CollectionWordItem} into the database.
     *
     * <p>Executed on a background disk I/O thread. The {@code onDone}
     * callback is delivered on the main thread once the insertion completes.</p>
     *
     * @param item   the word item to insert; must not be null
     * @param onDone a {@link Runnable} invoked on the main thread after insertion;
     *               must not be null
     */
    void addWord(@NonNull CollectionWordItem item, @NonNull Runnable onDone);

    /**
     * Returns a {@link LiveData} list of all words belonging to the specified
     * collection, ordered from newest to oldest by saved timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever the
     * underlying table changes.</p>
     *
     * @param collectionId the ID of the parent collection to query
     * @return a {@link LiveData} emitting the list of {@link CollectionWordItem} objects
     */
    LiveData<List<CollectionWordItem>> getWordsByCollection(long collectionId);

    /**
     * Returns a {@link LiveData} integer representing the total number of
     * words saved inside the specified collection.
     *
     * @param collectionId the ID of the parent collection to count
     * @return a {@link LiveData} emitting the current word count
     */
    LiveData<Integer> getWordCount(long collectionId);

    /**
     * Deletes a single word record identified by its database ID.
     *
     * <p>Executed on a background disk I/O thread.</p>
     *
     * @param wordId the unique database ID of the word record to delete
     */
    void deleteWord(long wordId);
}

package com.igoy86.nexttranslate.domain.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.CollectionItem;

import java.util.List;

/**
 * Repository interface defining the contract for user collection
 * persistence operations.
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * Room or any Android framework classes directly. The actual implementation
 * resides in the data layer
 * ({@link com.igoy86.nexttranslate.data.repository.CollectionRepositoryImpl}).</p>
 *
 * <p>All write operations are performed on a background thread via
 * {@link com.igoy86.nexttranslate.util.AppExecutors#diskIO()}.
 * Read operations return {@link LiveData} that Room updates automatically
 * whenever the underlying data changes.</p>
 */
public interface CollectionRepository {

    /**
     * Inserts a new {@link CollectionItem} into the database and delivers
     * the auto-generated row ID via {@link InsertCallback}.
     *
     * <p>Executed on a background disk I/O thread.</p>
     *
     * @param item     the collection to create; must not be null
     * @param callback receives the generated ID on the background thread
     */
    void createCollection(
            @NonNull CollectionItem item,
            @NonNull InsertCallback callback
    );

    /**
     * Returns a {@link LiveData} list of all user collections ordered
     * from newest to oldest by creation timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying data changes.</p>
     *
     * @return a {@link LiveData} emitting the full list of {@link CollectionItem} objects
     */
    LiveData<List<CollectionItem>> getAllCollections();

    /**
     * Renames an existing collection identified by its database ID.
     *
     * <p>Executed on a background disk I/O thread.</p>
     *
     * @param id      the unique database ID of the collection to rename
     * @param newName the new display name; must not be null
     */
    void renameCollection(long id, @NonNull String newName);

    /**
     * Deletes a single collection identified by its database ID.
     *
     * <p>Executed on a background disk I/O thread. If no collection with
     * the given ID exists, this operation is a no-op.</p>
     *
     * @param id the unique database ID of the collection to delete
     */
    void deleteCollection(long id);

    // =========================================================================
    // Callback interface
    // =========================================================================

    /**
     * Callback interface used to receive the auto-generated row ID after a
     * successful {@link #createCollection} operation.
     *
     * <p>The callback is invoked on the disk I/O background thread. Callers
     * that need to update UI or ViewModel state must post back to the main
     * thread via {@link com.igoy86.nexttranslate.util.AppExecutors#mainThread()}.</p>
     */
    interface InsertCallback {

        /**
         * Called when the insert operation completes successfully.
         *
         * @param generatedId the auto-generated primary key of the new row;
         *                    always &gt; 0 on success
         */
        void onInserted(long generatedId);

        /**
         * Called when the insert operation fails.
         *
         * @param errorMessage a human-readable description of the error
         */
        default void onError(@NonNull String errorMessage) {
            // Default no-op for backward compatibility
        }
    }
}

package com.igoy86.nexttranslate.domain.repository;

import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.FavoriteItem;

import java.util.List;

/**
 * Repository interface defining the contract for favorite translations
 * persistence operations.
 *
 * <p>This interface belongs to the domain layer and has no dependency on
 * Room or any Android framework classes directly. The actual implementation
 * resides in the data layer ({@code FavoriteRepositoryImpl}).</p>
 *
 * <p>All write operations (insert, delete) are performed on a background
 * thread via {@link com.igoy86.nexttranslate.util.AppExecutors}.
 * Read operations return {@link LiveData} that Room updates automatically
 * whenever the underlying data changes.</p>
 */
public interface FavoriteRepository {

    /**
     * Inserts a new {@link FavoriteItem} into the database.
     *
     * <p>Executed on a background disk I/O thread. If an entry with the
     * same primary key already exists, it will be replaced.</p>
     *
     * @param item the {@link FavoriteItem} to insert; must not be null
     */
    void addFavorite(FavoriteItem item);

    /**
     * Returns a {@link LiveData} list of all favorite translation entries,
     * ordered from newest to oldest by saved timestamp.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying favorites table changes.</p>
     *
     * @return a {@link LiveData} emitting the full list of {@link FavoriteItem} objects
     */
    LiveData<List<FavoriteItem>> getAllFavorites();

    /**
     * Deletes a single {@link FavoriteItem} from the database by its ID.
     *
     * <p>Executed on a background disk I/O thread. If no entry with the
     * given ID exists, this operation is a no-op.</p>
     *
     * @param id the unique database ID of the favorite entry to delete
     */
    void deleteFavorite(long id);
	
	 /**
     * Deletes all {@link FavoriteItem} entries from the database.
     *
     * <p>Executed on a background disk I/O thread.</p>
     */
    void clearAllFavorites();
}
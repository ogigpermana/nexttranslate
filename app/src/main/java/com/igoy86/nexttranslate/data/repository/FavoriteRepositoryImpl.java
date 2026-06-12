package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.igoy86.nexttranslate.data.local.dao.FavoriteDao;
import com.igoy86.nexttranslate.data.mapper.FavoriteMapper;
import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.domain.repository.FavoriteRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * Concrete implementation of {@link FavoriteRepository}.
 *
 * <p>Handles all favorite translation persistence operations using Room
 * via {@link FavoriteDao}. Converts between data layer entities and domain
 * models using {@link FavoriteMapper}.</p>
 *
 * <p>All write operations are dispatched to a background disk I/O thread
 * via {@link AppExecutors#diskIO()} to avoid blocking the main thread.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class FavoriteRepositoryImpl implements FavoriteRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "FavoriteRepositoryImpl";

    /** DAO used to perform Room database operations on the favorites table. */
    @NonNull
    private final FavoriteDao favoriteDao;

    /** Executor pools used to dispatch background and main-thread tasks. */
    @NonNull
    private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link FavoriteRepositoryImpl} with the required dependencies.
     *
     * @param favoriteDao  the DAO for favorites table operations; must not be null
     * @param appExecutors the executor pools for background threading; must not be null
     */
    public FavoriteRepositoryImpl(
            @NonNull FavoriteDao favoriteDao,
            @NonNull AppExecutors appExecutors
    ) {
        this.favoriteDao = favoriteDao;
        this.appExecutors = appExecutors;
    }

    // -------------------------------------------------------------------------
    // FavoriteRepository implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Converts the given {@link FavoriteItem} to a
     * {@link com.igoy86.nexttranslate.data.local.entity.FavoriteEntity}
     * via {@link FavoriteMapper} and inserts it on a background disk I/O thread.</p>
     */
    @Override
    public void addFavorite(@NonNull FavoriteItem item) {
        appExecutors.diskIO().execute(() -> {
            try {
                favoriteDao.insert(FavoriteMapper.toEntity(item));
                FileLogger.d(TAG, "Favorite inserted: " + item.getSourceText());
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to insert favorite entry.", ex);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@link androidx.lifecycle.Transformations#map} to convert the
     * {@link LiveData} list of {@link com.igoy86.nexttranslate.data.local.entity.FavoriteEntity}
     * objects emitted by Room into a {@link LiveData} list of {@link FavoriteItem}
     * domain models before returning it to the caller.</p>
     */
    @Override
    public LiveData<List<FavoriteItem>> getAllFavorites() {
        return Transformations.map(
                favoriteDao.getAllFavorites(),
                FavoriteMapper::toDomainList
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the delete operation to a background disk I/O thread.</p>
     */
    @Override
    public void deleteFavorite(long id) {
        appExecutors.diskIO().execute(() -> {
            try {
                favoriteDao.deleteById(id);
                FileLogger.d(TAG, "Favorite deleted: id=" + id);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to delete favorite entry: id=" + id, ex);
            }
        });
    }
	
	/**
     * {@inheritDoc}
     *
     * <p>Dispatches the clear-all operation to a background disk I/O thread.</p>
     */
    @Override
    public void clearAllFavorites() {
        appExecutors.diskIO().execute(() -> {
            try {
                favoriteDao.clearAll();
                FileLogger.d(TAG, "All favorites cleared.");
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to clear all favorites.", ex);
            }
        });
    }
}
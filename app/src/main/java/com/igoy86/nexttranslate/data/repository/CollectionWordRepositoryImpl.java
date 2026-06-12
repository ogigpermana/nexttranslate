package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.igoy86.nexttranslate.data.local.dao.CollectionWordDao;
import com.igoy86.nexttranslate.data.mapper.CollectionWordMapper;
import com.igoy86.nexttranslate.domain.model.CollectionWordItem;
import com.igoy86.nexttranslate.domain.repository.CollectionWordRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * Concrete implementation of {@link CollectionWordRepository}.
 *
 * <p>Handles all word persistence operations for user collections using
 * Room via {@link CollectionWordDao}. Converts between data layer entities
 * and domain models using {@link CollectionWordMapper}.</p>
 *
 * <p>All write operations are dispatched to a background disk I/O thread
 * via {@link AppExecutors#diskIO()} to avoid blocking the main thread.
 * Callbacks that need to deliver results to the UI are posted back to the
 * main thread via {@link AppExecutors#mainThread()}.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class CollectionWordRepositoryImpl implements CollectionWordRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "CollectionWordRepositoryImpl";

    /** DAO used to perform Room database operations on the collection_words table. */
    @NonNull
    private final CollectionWordDao collectionWordDao;

    /** Executor pools used to dispatch background and main-thread tasks. */
    @NonNull
    private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link CollectionWordRepositoryImpl} with the required dependencies.
     *
     * @param collectionWordDao the DAO for collection word table operations; must not be null
     * @param appExecutors      the executor pools for background threading; must not be null
     */
    public CollectionWordRepositoryImpl(
            @NonNull CollectionWordDao collectionWordDao,
            @NonNull AppExecutors appExecutors
    ) {
        this.collectionWordDao = collectionWordDao;
        this.appExecutors = appExecutors;
    }

    // -------------------------------------------------------------------------
    // CollectionWordRepository implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Converts the given {@link CollectionWordItem} to a
     * {@link com.igoy86.nexttranslate.data.local.entity.CollectionWordEntity}
     * via {@link CollectionWordMapper}, inserts it on a background disk I/O thread,
     * and delivers the {@code onDone} callback on the main thread.</p>
     */
    @Override
    public void addWord(@NonNull CollectionWordItem item, @NonNull Runnable onDone) {
        appExecutors.diskIO().execute(() -> {
            try {
                final long generatedId = collectionWordDao.insert(
                        CollectionWordMapper.toEntity(item)
                );
                FileLogger.d(TAG, "Word inserted: id=" + generatedId
                        + " word=" + item.getWord()
                        + " collectionId=" + item.getCollectionId());
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to insert word: " + item.getWord(), ex);
            } finally {
                appExecutors.mainThread().execute(onDone);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@link Transformations#map} to convert the {@link LiveData} list of
     * {@link com.igoy86.nexttranslate.data.local.entity.CollectionWordEntity} objects
     * emitted by Room into a {@link LiveData} list of {@link CollectionWordItem}
     * domain models before returning it to the caller.</p>
     */
    @Override
    public LiveData<List<CollectionWordItem>> getWordsByCollection(long collectionId) {
        return Transformations.map(
                collectionWordDao.getWordsByCollection(collectionId),
                CollectionWordMapper::toDomainList
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LiveData<Integer> getWordCount(long collectionId) {
        return collectionWordDao.getWordCount(collectionId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the delete operation to a background disk I/O thread.</p>
     */
    @Override
    public void deleteWord(long wordId) {
        appExecutors.diskIO().execute(() -> {
            try {
                collectionWordDao.deleteById(wordId);
                FileLogger.d(TAG, "Word deleted: id=" + wordId);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to delete word: id=" + wordId, ex);
            }
        });
    }
}

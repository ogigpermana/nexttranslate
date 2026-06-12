package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.igoy86.nexttranslate.data.local.dao.CollectionDao;
import com.igoy86.nexttranslate.data.local.dto.CollectionWithWordCount;
import com.igoy86.nexttranslate.data.mapper.CollectionMapper;
import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.repository.CollectionRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of {@link CollectionRepository}.
 *
 * <p>Handles all user collection persistence operations using Room via
 * {@link CollectionDao}. Converts between data layer entities and domain
 * models using {@link CollectionMapper}.</p>
 *
 * <p>The {@link #getAllCollections()} method uses a LEFT JOIN query that
 * includes the word count per collection, so the adapter can display
 * the correct count badge without additional queries.</p>
 *
 * <p>All write operations are dispatched to a background disk I/O thread
 * via {@link AppExecutors#diskIO()} to avoid blocking the main thread.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class CollectionRepositoryImpl implements CollectionRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "CollectionRepositoryImpl";

    /** DAO used to perform Room database operations on the collections table. */
    @NonNull
    private final CollectionDao collectionDao;

    /** Executor pools used to dispatch background and main-thread tasks. */
    @NonNull
    private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link CollectionRepositoryImpl} with the required dependencies.
     *
     * @param collectionDao the DAO for collection table operations; must not be null
     * @param appExecutors  the executor pools for background threading; must not be null
     */
    public CollectionRepositoryImpl(
            @NonNull CollectionDao collectionDao,
            @NonNull AppExecutors appExecutors
    ) {
        this.collectionDao = collectionDao;
        this.appExecutors = appExecutors;
    }

    // -------------------------------------------------------------------------
    // CollectionRepository implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Converts the given {@link CollectionItem} to a
     * {@link com.igoy86.nexttranslate.data.local.entity.CollectionEntity}
     * via {@link CollectionMapper}, inserts it on a background disk I/O thread,
     * and delivers the generated ID via {@code callback}.</p>
     */
    @Override
    public void createCollection(
            @NonNull CollectionItem item,
            @NonNull InsertCallback callback
    ) {
        appExecutors.diskIO().execute(() -> {
            try {
                final long generatedId = collectionDao.insert(
                        CollectionMapper.toEntity(item)
                );
                FileLogger.d(TAG, "Collection created: id=" + generatedId
                        + " name=" + item.getName());
                callback.onInserted(generatedId);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to create collection: " + item.getName(), ex);
                callback.onError("Failed to create collection: " + ex.getMessage());
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@code getAllCollectionsWithWordCount()} — a LEFT JOIN query that
     * returns each collection together with the number of words saved inside it.
     * The result is mapped to a list of {@link CollectionItem} domain models
     * via {@link Transformations#map}, with each item's {@code wordCount} populated
     * from the JOIN result so the UI can display the correct badge immediately.</p>
     */
    @Override
    public LiveData<List<CollectionItem>> getAllCollections() {
        return Transformations.map(
                collectionDao.getAllCollectionsWithWordCount(),
                dtos -> {
                    final List<CollectionItem> items = new ArrayList<>();
                    if (dtos == null) return items;
                    for (CollectionWithWordCount dto : dtos) {
                        items.add(CollectionMapper.toDomain(dto.collection, dto.wordCount));
                    }
                    return items;
                }
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the rename operation to a background disk I/O thread.</p>
     */
    @Override
    public void renameCollection(long id, @NonNull String newName) {
        appExecutors.diskIO().execute(() -> {
            try {
                collectionDao.rename(id, newName);
                FileLogger.d(TAG, "Collection renamed: id=" + id + " newName=" + newName);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to rename collection: id=" + id, ex);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the delete operation to a background disk I/O thread.</p>
     */
    @Override
    public void deleteCollection(long id) {
        appExecutors.diskIO().execute(() -> {
            try {
                collectionDao.deleteById(id);
                FileLogger.d(TAG, "Collection deleted: id=" + id);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to delete collection: id=" + id, ex);
            }
        });
    }
}

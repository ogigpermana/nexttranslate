package com.igoy86.nexttranslate.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.igoy86.nexttranslate.data.local.dao.HistoryDao;
import com.igoy86.nexttranslate.data.mapper.HistoryMapper;
import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.domain.repository.HistoryRepository;
import com.igoy86.nexttranslate.util.AppExecutors;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * Concrete implementation of {@link HistoryRepository}.
 *
 * <p>Handles all translation history persistence operations using Room
 * via {@link HistoryDao}. Converts between data layer entities and domain
 * models using {@link HistoryMapper}.</p>
 *
 * <p>All write operations are dispatched to a background disk I/O thread
 * via {@link AppExecutors#diskIO()} to avoid blocking the main thread.</p>
 *
 * <p>This class belongs to the data layer and is instantiated by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class HistoryRepositoryImpl implements HistoryRepository {

    /** Tag used for logging events originating from this repository. */
    private static final String TAG = "HistoryRepositoryImpl";

    /** DAO used to perform Room database operations on the history table. */
    @NonNull
    private final HistoryDao historyDao;

    /** Executor pools used to dispatch background and main-thread tasks. */
    @NonNull
    private final AppExecutors appExecutors;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link HistoryRepositoryImpl} with the required dependencies.
     *
     * @param historyDao   the DAO for history table operations; must not be null
     * @param appExecutors the executor pools for background threading; must not be null
     */
    public HistoryRepositoryImpl(
            @NonNull HistoryDao historyDao,
            @NonNull AppExecutors appExecutors
    ) {
        this.historyDao = historyDao;
        this.appExecutors = appExecutors;
    }

    // -------------------------------------------------------------------------
    // HistoryRepository implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Converts the given {@link HistoryItem} to a {@link com.igoy86.nexttranslate.data.local.entity.HistoryEntity}
     * via {@link HistoryMapper} and inserts it on a background disk I/O thread.</p>
     */
    @Override
    public void addHistory(@NonNull HistoryItem item) {
        appExecutors.diskIO().execute(() -> {
            try {
                historyDao.insert(HistoryMapper.toEntity(item));
                FileLogger.d(TAG, "History inserted: " + item.getSourceText());
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to insert history entry.", ex);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@link androidx.lifecycle.Transformations#map} to convert the
     * {@link LiveData} list of {@link com.igoy86.nexttranslate.data.local.entity.HistoryEntity}
     * objects emitted by Room into a {@link LiveData} list of {@link HistoryItem}
     * domain models before returning it to the caller.</p>
     */
    @Override
    public LiveData<List<HistoryItem>> getAllHistory() {
        return Transformations.map(
                historyDao.getAllHistory(),
                HistoryMapper::toDomainList
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the delete operation to a background disk I/O thread.</p>
     */
    @Override
    public void deleteHistory(long id) {
        appExecutors.diskIO().execute(() -> {
            try {
                historyDao.deleteById(id);
                FileLogger.d(TAG, "History deleted: id=" + id);
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to delete history entry: id=" + id, ex);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Dispatches the clear operation to a background disk I/O thread.</p>
     */
    @Override
    public void clearAllHistory() {
        appExecutors.diskIO().execute(() -> {
            try {
                historyDao.clearAll();
                FileLogger.d(TAG, "All history cleared.");
            } catch (Exception ex) {
                FileLogger.e(TAG, "Failed to clear history.", ex);
            }
        });
    }
}
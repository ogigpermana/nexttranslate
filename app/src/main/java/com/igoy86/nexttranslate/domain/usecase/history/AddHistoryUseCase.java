package com.igoy86.nexttranslate.domain.usecase.history;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.domain.repository.HistoryRepository;

/**
 * Use case responsible for adding a new entry to the translation history.
 *
 * <p>Encapsulates the single business rule: whenever a translation is completed,
 * a {@link HistoryItem} is persisted to the database for later retrieval.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link HistoryRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     AddHistoryUseCase addHistoryUseCase = container.getAddHistoryUseCase();
 *     addHistoryUseCase.execute(new HistoryItem(0, "Hello", "Halo", "en", "id", timestamp));
 * </pre>
 */
public class AddHistoryUseCase {

    /** Repository used to persist the history entry. */
    @NonNull
    private final HistoryRepository historyRepository;

    /**
     * Constructs a new {@link AddHistoryUseCase} with the given repository.
     *
     * @param historyRepository the repository used to insert history entries;
     *                          must not be null
     */
    public AddHistoryUseCase(@NonNull HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Executes the use case by inserting the given {@link HistoryItem}
     * into the database (fire-and-forget, no ID returned).
     *
     * <p>The actual database write is performed on a background disk I/O
     * thread managed by {@link com.igoy86.nexttranslate.util.AppExecutors}.
     * This method returns immediately without blocking the caller.</p>
     *
     * @param item the history entry to persist; must not be null
     */
    public void execute(@NonNull HistoryItem item) {
        historyRepository.addHistory(item);
    }

    /**
     * Inserts the given {@link HistoryItem} and delivers the auto-generated
     * row ID via {@code callback} on the disk I/O thread.
     *
     * <p>Used by the upsert pattern in {@code TranslateViewModel} to track
     * the current session's history entry ID for subsequent UPDATE calls via
     * {@link UpdateHistoryUseCase}. The callback is invoked on a background
     * thread — the ViewModel posts back to the main thread internally.</p>
     *
     * @param item     the history entry to insert; must not be null
     * @param callback receives the generated ID; invoked on background thread;
     *                 must not be null
     */
    public void insertAndGetId(
            @NonNull HistoryItem item,
            @NonNull HistoryRepository.InsertCallback callback
    ) {
        historyRepository.insertAndGetId(item, callback);
    }
}

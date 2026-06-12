package com.igoy86.nexttranslate.domain.usecase.history;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.domain.repository.HistoryRepository;

/**
 * Use case responsible for restoring a previously deleted {@link HistoryItem}
 * back into the database.
 *
 * <p>Used exclusively by the Undo action in the swipe-to-delete Snackbar
 * in {@link com.igoy86.nexttranslate.presentation.history.HistoryFragment}.
 * Re-inserts the item with its original data (including the original ID)
 * so the entry appears exactly as it was before deletion.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link HistoryRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     RestoreHistoryUseCase restoreHistoryUseCase = container.getRestoreHistoryUseCase();
 *     restoreHistoryUseCase.execute(deletedItem);
 * </pre>
 */
public class RestoreHistoryUseCase {

    /** Repository used to re-insert the history entry. */
    @NonNull
    private final HistoryRepository historyRepository;

    /**
     * Constructs a new {@link RestoreHistoryUseCase} with the given repository.
     *
     * @param historyRepository the repository used to restore history entries;
     *                          must not be null
     */
    public RestoreHistoryUseCase(@NonNull HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Executes the use case by re-inserting the given {@link HistoryItem}
     * into the database.
     *
     * <p>The item is inserted via {@link HistoryRepository#addHistory}, which
     * uses REPLACE conflict strategy — so if the original ID still exists
     * (edge case), it is safely overwritten with the same data.</p>
     *
     * <p>The operation is performed on a background disk I/O thread managed
     * by the repository implementation.</p>
     *
     * @param item the {@link HistoryItem} to restore; must not be null
     */
    public void execute(@NonNull HistoryItem item) {
        historyRepository.addHistory(item);
    }
}
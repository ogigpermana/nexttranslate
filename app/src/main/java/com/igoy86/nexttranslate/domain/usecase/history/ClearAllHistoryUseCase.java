package com.igoy86.nexttranslate.domain.usecase.history;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.HistoryRepository;

/**
 * Use case responsible for clearing all translation history entries
 * from the database.
 *
 * <p>This is a destructive operation and should only be triggered by
 * deliberate user action, such as tapping a "Clear All History" button
 * with a confirmation dialog.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link HistoryRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     ClearAllHistoryUseCase clearAllHistoryUseCase = container.getClearAllHistoryUseCase();
 *     clearAllHistoryUseCase.execute();
 * </pre>
 */
public class ClearAllHistoryUseCase {

    /** Repository used to clear all history entries. */
    @NonNull
    private final HistoryRepository historyRepository;

    /**
     * Constructs a new {@link ClearAllHistoryUseCase} with the given repository.
     *
     * @param historyRepository the repository used to clear history entries
     */
    public ClearAllHistoryUseCase(@NonNull HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Executes the use case by deleting all history entries from the database.
     *
     * <p>The actual database operation is performed on a background disk I/O
     * thread managed by {@link com.igoy86.nexttranslate.util.AppExecutors}.
     * This operation is irreversible.</p>
     */
    public void execute() {
        historyRepository.clearAllHistory();
    }
}
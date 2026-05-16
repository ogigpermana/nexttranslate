package com.igoy86.nexttranslate.domain.usecase.history;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.HistoryRepository;

/**
 * Use case responsible for deleting a single translation history entry
 * identified by its database ID.
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link HistoryRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     DeleteHistoryUseCase deleteHistoryUseCase = container.getDeleteHistoryUseCase();
 *     deleteHistoryUseCase.execute(historyItem.getId());
 * </pre>
 */
public class DeleteHistoryUseCase {

    /** Repository used to delete the history entry. */
    @NonNull
    private final HistoryRepository historyRepository;

    /**
     * Constructs a new {@link DeleteHistoryUseCase} with the given repository.
     *
     * @param historyRepository the repository used to delete history entries
     */
    public DeleteHistoryUseCase(@NonNull HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Executes the use case by deleting the history entry with the given ID.
     *
     * <p>The actual database delete is performed on a background disk I/O
     * thread managed by {@link com.igoy86.nexttranslate.util.AppExecutors}.
     * If no entry with the given ID exists, this is a no-op.</p>
     *
     * @param id the unique database ID of the history entry to delete
     */
    public void execute(long id) {
        historyRepository.deleteHistory(id);
    }
}
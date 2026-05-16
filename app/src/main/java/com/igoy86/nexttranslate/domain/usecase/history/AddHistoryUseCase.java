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
     * @param historyRepository the repository used to insert history entries
     */
    public AddHistoryUseCase(@NonNull HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Executes the use case by inserting the given {@link HistoryItem}
     * into the database.
     *
     * <p>The actual database write is performed on a background disk I/O
     * thread managed by {@link com.igoy86.nexttranslate.util.AppExecutors}.</p>
     *
     * @param item the history entry to persist; must not be null
     */
    public void execute(@NonNull HistoryItem item) {
        historyRepository.addHistory(item);
    }
}
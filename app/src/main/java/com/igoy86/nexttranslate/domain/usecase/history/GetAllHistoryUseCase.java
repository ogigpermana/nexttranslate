package com.igoy86.nexttranslate.domain.usecase.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.domain.repository.HistoryRepository;

import java.util.List;

/**
 * Use case responsible for retrieving all translation history entries.
 *
 * <p>Returns a {@link LiveData} list that is automatically updated by Room
 * whenever the history table changes, enabling reactive UI updates without
 * manual polling.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link HistoryRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     GetAllHistoryUseCase getAllHistoryUseCase = container.getGetAllHistoryUseCase();
 *     getAllHistoryUseCase.execute().observe(this, historyList -> {
 *         // update RecyclerView adapter
 *     });
 * </pre>
 */
public class GetAllHistoryUseCase {

    /** Repository used to retrieve history entries. */
    @NonNull
    private final HistoryRepository historyRepository;

    /**
     * Constructs a new {@link GetAllHistoryUseCase} with the given repository.
     *
     * @param historyRepository the repository used to fetch history entries
     */
    public GetAllHistoryUseCase(@NonNull HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Executes the use case by returning a {@link LiveData} list of all
     * history entries ordered from newest to oldest.
     *
     * @return a {@link LiveData} emitting the full list of {@link HistoryItem} objects
     */
    @NonNull
    public LiveData<List<HistoryItem>> execute() {
        return historyRepository.getAllHistory();
    }
}
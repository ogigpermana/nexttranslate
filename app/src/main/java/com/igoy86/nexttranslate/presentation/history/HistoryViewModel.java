package com.igoy86.nexttranslate.presentation.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.HistoryItem;
import com.igoy86.nexttranslate.domain.usecase.history.ClearAllHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.DeleteHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.GetAllHistoryUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseViewModel;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * ViewModel for the translation history screen.
 *
 * <p>Manages the UI state for displaying, deleting, and clearing all
 * translation history entries. Survives configuration changes and exposes
 * state via {@link LiveData}.</p>
 *
 * <p>Depends on:</p>
 * <ul>
 *     <li>{@link GetAllHistoryUseCase} — retrieves all history entries</li>
 *     <li>{@link DeleteHistoryUseCase} — deletes a single history entry</li>
 *     <li>{@link ClearAllHistoryUseCase} — clears all history entries</li>
 * </ul>
 *
 * <p>Instantiated via {@link HistoryViewModelFactory}.</p>
 */
public class HistoryViewModel extends BaseViewModel {

    /** Tag used for logging events originating from this ViewModel. */
    private static final String TAG = "HistoryViewModel";

    // -------------------------------------------------------------------------
    // Use Cases
    // -------------------------------------------------------------------------

    /** Use case for retrieving all translation history entries. */
    @NonNull
    private final GetAllHistoryUseCase getAllHistoryUseCase;

    /** Use case for deleting a single translation history entry. */
    @NonNull
    private final DeleteHistoryUseCase deleteHistoryUseCase;

    /** Use case for clearing all translation history entries. */
    @NonNull
    private final ClearAllHistoryUseCase clearAllHistoryUseCase;

    // -------------------------------------------------------------------------
    // UI State LiveData
    // -------------------------------------------------------------------------

    /**
     * The complete list of translation history entries ordered newest to oldest.
     * Room automatically updates this LiveData whenever the history table changes.
     */
    @NonNull
    private final LiveData<List<HistoryItem>> historyListLiveData;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link HistoryViewModel} with the required use cases.
     *
     * <p>Immediately subscribes to the history list LiveData so that
     * observers receive updates as soon as they attach.</p>
     *
     * @param getAllHistoryUseCase   use case for retrieving history entries
     * @param deleteHistoryUseCase  use case for deleting a single entry
     * @param clearAllHistoryUseCase use case for clearing all entries
     */
    public HistoryViewModel(
            @NonNull GetAllHistoryUseCase getAllHistoryUseCase,
            @NonNull DeleteHistoryUseCase deleteHistoryUseCase,
            @NonNull ClearAllHistoryUseCase clearAllHistoryUseCase
    ) {
        this.getAllHistoryUseCase = getAllHistoryUseCase;
        this.deleteHistoryUseCase = deleteHistoryUseCase;
        this.clearAllHistoryUseCase = clearAllHistoryUseCase;
        this.historyListLiveData = getAllHistoryUseCase.execute();
        FileLogger.d(TAG, "HistoryViewModel initialized.");
    }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Deletes a single translation history entry identified by its database ID.
     *
     * <p>The operation is dispatched to a background disk I/O thread.
     * Room will automatically update {@link #getHistoryListLiveData()}
     * after the deletion completes.</p>
     *
     * @param id the unique database ID of the history entry to delete
     */
    public void deleteHistory(long id) {
        deleteHistoryUseCase.execute(id);
        FileLogger.d(TAG, "Delete history requested: id=" + id);
    }

    /**
     * Clears all translation history entries from the database.
     *
     * <p>This is a destructive and irreversible operation. Should only
     * be triggered after the user confirms via a dialog. Room will
     * automatically update {@link #getHistoryListLiveData()} after
     * the operation completes.</p>
     */
    public void clearAllHistory() {
        clearAllHistoryUseCase.execute();
        FileLogger.d(TAG, "Clear all history requested.");
    }

    // -------------------------------------------------------------------------
    // LiveData accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link LiveData} emitting the full list of translation
     * history entries ordered from newest to oldest.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying history table changes.</p>
     *
     * @return {@link LiveData} of the history entry list
     */
    @NonNull
    public LiveData<List<HistoryItem>> getHistoryListLiveData() {
        return historyListLiveData;
    }
}
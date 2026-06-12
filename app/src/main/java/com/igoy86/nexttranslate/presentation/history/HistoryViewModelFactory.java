package com.igoy86.nexttranslate.presentation.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.igoy86.nexttranslate.domain.usecase.history.ClearAllHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.DeleteHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.GetAllHistoryUseCase;
import com.igoy86.nexttranslate.domain.usecase.history.RestoreHistoryUseCase;
import com.igoy86.nexttranslate.util.FileLogger;

/**
 * Factory class responsible for creating instances of {@link HistoryViewModel}.
 *
 * <p>Provides the required use case dependencies to {@link HistoryViewModel}
 * while integrating with Android's {@link ViewModelProvider} API.</p>
 *
 * <p>Usage example in a Fragment:</p>
 * <pre>
 *     AppContainer container = NextTranslateApp.getContainer();
 *
 *     HistoryViewModelFactory factory = new HistoryViewModelFactory(
 *             container.getGetAllHistoryUseCase(),
 *             container.getDeleteHistoryUseCase(),
 *             container.getClearAllHistoryUseCase(),
 *             container.getRestoreHistoryUseCase()
 *     );
 *
 *     HistoryViewModel viewModel = new ViewModelProvider(this, factory)
 *             .get(HistoryViewModel.class);
 * </pre>
 */
public class HistoryViewModelFactory implements ViewModelProvider.Factory {

    /** Tag used for logging events originating from this factory. */
    private static final String TAG = "HistoryVMFactory";

    /** Use case for retrieving all translation history entries. */
    @NonNull
    private final GetAllHistoryUseCase getAllHistoryUseCase;

    /** Use case for deleting a single translation history entry. */
    @NonNull
    private final DeleteHistoryUseCase deleteHistoryUseCase;

    /** Use case for clearing all translation history entries. */
    @NonNull
    private final ClearAllHistoryUseCase clearAllHistoryUseCase;

    /** Use case for restoring a deleted history entry (Undo swipe-delete). */
    @NonNull
    private final RestoreHistoryUseCase restoreHistoryUseCase;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link HistoryViewModelFactory} with all required
     * use case dependencies.
     *
     * @param getAllHistoryUseCase    use case for retrieving history; must not be null
     * @param deleteHistoryUseCase   use case for deleting a single entry; must not be null
     * @param clearAllHistoryUseCase use case for clearing all entries; must not be null
     * @param restoreHistoryUseCase  use case for restoring a deleted entry; must not be null
     */
    public HistoryViewModelFactory(
            @NonNull GetAllHistoryUseCase getAllHistoryUseCase,
            @NonNull DeleteHistoryUseCase deleteHistoryUseCase,
            @NonNull ClearAllHistoryUseCase clearAllHistoryUseCase,
            @NonNull RestoreHistoryUseCase restoreHistoryUseCase
    ) {
        this.getAllHistoryUseCase = getAllHistoryUseCase;
        this.deleteHistoryUseCase = deleteHistoryUseCase;
        this.clearAllHistoryUseCase = clearAllHistoryUseCase;
        this.restoreHistoryUseCase = restoreHistoryUseCase;
    }

    // -------------------------------------------------------------------------
    // ViewModelProvider.Factory implementation
    // -------------------------------------------------------------------------

    /**
     * Creates a new instance of the requested {@link ViewModel} class.
     *
     * <p>Only {@link HistoryViewModel} is supported by this factory.
     * Requesting any other ViewModel class will throw an
     * {@link IllegalArgumentException}.</p>
     *
     * @param modelClass the class of the ViewModel to create; must not be null
     * @param <T>        the type of the ViewModel
     * @return a newly created {@link HistoryViewModel} instance
     * @throws IllegalArgumentException if {@code modelClass} is not
     *                                  {@link HistoryViewModel}
     */
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(HistoryViewModel.class)) {
            FileLogger.d(TAG, "Creating HistoryViewModel instance.");

            //noinspection unchecked
            return (T) new HistoryViewModel(
                    getAllHistoryUseCase,
                    deleteHistoryUseCase,
                    clearAllHistoryUseCase,
                    restoreHistoryUseCase
            );
        }
        throw new IllegalArgumentException(
                "HistoryViewModelFactory cannot create an instance of: "
                        + modelClass.getName()
        );
    }
}
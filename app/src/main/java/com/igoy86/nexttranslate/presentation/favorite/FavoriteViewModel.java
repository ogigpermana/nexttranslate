package com.igoy86.nexttranslate.presentation.favorite;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.domain.usecase.favorite.ClearAllFavoritesUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.DeleteFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.GetAllFavoritesUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.RestoreFavoriteUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseViewModel;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * ViewModel for the favorite translations screen.
 *
 * <p>Manages the UI state for displaying, deleting, restoring, and clearing
 * favorite translation entries. Survives configuration changes and exposes
 * state via {@link LiveData}.</p>
 *
 * <p>Depends on:</p>
 * <ul>
 *     <li>{@link GetAllFavoritesUseCase}    — retrieves all favorite entries</li>
 *     <li>{@link DeleteFavoriteUseCase}     — deletes a single favorite entry</li>
 *     <li>{@link RestoreFavoriteUseCase}    — restores a deleted entry (Undo)</li>
 *     <li>{@link ClearAllFavoritesUseCase}  — clears all favorite entries</li>
 * </ul>
 *
 * <p>Instantiated via {@link FavoriteViewModelFactory}.</p>
 */
public class FavoriteViewModel extends BaseViewModel {

    /** Tag used for logging events originating from this ViewModel. */
    private static final String TAG = "FavoriteViewModel";

    // -------------------------------------------------------------------------
    // Use Cases
    // -------------------------------------------------------------------------

    /** Use case for retrieving all favorite translation entries. */
    @NonNull
    private final GetAllFavoritesUseCase getAllFavoritesUseCase;

    /** Use case for deleting a single favorite translation entry. */
    @NonNull
    private final DeleteFavoriteUseCase deleteFavoriteUseCase;

    /** Use case for restoring a deleted favorite entry (Undo swipe-delete). */
    @NonNull
    private final RestoreFavoriteUseCase restoreFavoriteUseCase;

    /** Use case for clearing all favorite entries at once. */
    @NonNull
    private final ClearAllFavoritesUseCase clearAllFavoritesUseCase;

    // -------------------------------------------------------------------------
    // UI State LiveData
    // -------------------------------------------------------------------------

    /**
     * The complete list of favorite translation entries ordered newest to oldest.
     * Room automatically updates this LiveData whenever the favorites table changes.
     */
    @NonNull
    private final LiveData<List<FavoriteItem>> favoritesListLiveData;

    /**
     * One-shot Snackbar message LiveData.
     * Consumed by {@link FavoriteFragment} and cleared via
     * {@link #clearSnackbarMessage()} after display.
     */
    @NonNull
    private final MutableLiveData<String> snackbarMessageLiveData = new MutableLiveData<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link FavoriteViewModel} with all required use cases.
     *
     * @param getAllFavoritesUseCase   use case for retrieving favorites; must not be null
     * @param deleteFavoriteUseCase   use case for deleting a single entry; must not be null
     * @param restoreFavoriteUseCase  use case for restoring a deleted entry; must not be null
     * @param clearAllFavoritesUseCase use case for clearing all entries; must not be null
     */
    public FavoriteViewModel(
            @NonNull GetAllFavoritesUseCase getAllFavoritesUseCase,
            @NonNull DeleteFavoriteUseCase deleteFavoriteUseCase,
            @NonNull RestoreFavoriteUseCase restoreFavoriteUseCase,
            @NonNull ClearAllFavoritesUseCase clearAllFavoritesUseCase
    ) {
        this.getAllFavoritesUseCase = getAllFavoritesUseCase;
        this.deleteFavoriteUseCase = deleteFavoriteUseCase;
        this.restoreFavoriteUseCase = restoreFavoriteUseCase;
        this.clearAllFavoritesUseCase = clearAllFavoritesUseCase;
        this.favoritesListLiveData = getAllFavoritesUseCase.execute();
        FileLogger.d(TAG, "FavoriteViewModel initialized.");
    }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Deletes a single favorite entry by its database ID.
     *
     * @param id the unique database ID of the entry to delete
     */
    public void deleteFavorite(long id) {
        deleteFavoriteUseCase.execute(id);
        FileLogger.d(TAG, "Delete favorite requested: id=" + id);
    }

    /**
     * Restores a previously deleted {@link FavoriteItem} back into the database.
     * Called when the user taps Undo on the swipe-delete Snackbar.
     *
     * @param item the {@link FavoriteItem} to restore; must not be null
     */
    public void restoreFavorite(@NonNull FavoriteItem item) {
        restoreFavoriteUseCase.execute(item);
        FileLogger.d(TAG, "Restore favorite requested: id=" + item.getId());
    }

    /**
     * Clears all favorite entries from the database.
     * Shows a Snackbar message after clearing.
     */
    public void clearAllFavorites() {
        clearAllFavoritesUseCase.execute();
        snackbarMessageLiveData.setValue("All favorites cleared.");
        FileLogger.d(TAG, "Clear all favorites requested.");
    }

    /**
     * Clears the one-shot Snackbar message after it has been displayed.
     */
    public void clearSnackbarMessage() {
        snackbarMessageLiveData.setValue(null);
    }

    // -------------------------------------------------------------------------
    // LiveData accessors
    // -------------------------------------------------------------------------

    /** @return {@link LiveData} of the full favorites list, newest first */
    @NonNull
    public LiveData<List<FavoriteItem>> getFavoritesListLiveData() {
        return favoritesListLiveData;
    }

    /** @return one-shot Snackbar message LiveData */
    @NonNull
    public LiveData<String> getSnackbarMessageLiveData() {
        return snackbarMessageLiveData;
    }
}
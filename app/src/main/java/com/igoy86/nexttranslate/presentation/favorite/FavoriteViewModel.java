package com.igoy86.nexttranslate.presentation.favorite;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.domain.usecase.favorite.DeleteFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.GetAllFavoritesUseCase;
import com.igoy86.nexttranslate.presentation.base.BaseViewModel;
import com.igoy86.nexttranslate.util.FileLogger;

import java.util.List;

/**
 * ViewModel for the favorite translations screen.
 *
 * <p>Manages the UI state for displaying and deleting favorite translation
 * entries. Survives configuration changes and exposes state via
 * {@link LiveData}.</p>
 *
 * <p>Depends on:</p>
 * <ul>
 *     <li>{@link GetAllFavoritesUseCase} — retrieves all favorite entries</li>
 *     <li>{@link DeleteFavoriteUseCase} — deletes a single favorite entry</li>
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

    // -------------------------------------------------------------------------
    // UI State LiveData
    // -------------------------------------------------------------------------

    /**
     * The complete list of favorite translation entries ordered newest to oldest.
     * Room automatically updates this LiveData whenever the favorites table changes.
     */
    @NonNull
    private final LiveData<List<FavoriteItem>> favoritesListLiveData;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link FavoriteViewModel} with the required use cases.
     *
     * <p>Immediately subscribes to the favorites list LiveData so that
     * observers receive updates as soon as they attach.</p>
     *
     * @param getAllFavoritesUseCase use case for retrieving favorites; must not be null
     * @param deleteFavoriteUseCase use case for deleting a single entry; must not be null
     */
    public FavoriteViewModel(
            @NonNull GetAllFavoritesUseCase getAllFavoritesUseCase,
            @NonNull DeleteFavoriteUseCase deleteFavoriteUseCase
    ) {
        this.getAllFavoritesUseCase = getAllFavoritesUseCase;
        this.deleteFavoriteUseCase = deleteFavoriteUseCase;
        this.favoritesListLiveData = getAllFavoritesUseCase.execute();
        FileLogger.d(TAG, "FavoriteViewModel initialized.");
    }

    // -------------------------------------------------------------------------
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Deletes a single favorite translation entry identified by its database ID.
     *
     * <p>The operation is dispatched to a background disk I/O thread.
     * Room will automatically update {@link #getFavoritesListLiveData()}
     * after the deletion completes.</p>
     *
     * @param id the unique database ID of the favorite entry to delete
     */
    public void deleteFavorite(long id) {
        deleteFavoriteUseCase.execute(id);
        FileLogger.d(TAG, "Delete favorite requested: id=" + id);
    }

    // -------------------------------------------------------------------------
    // LiveData accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link LiveData} emitting the full list of favorite
     * translation entries ordered from newest to oldest.
     *
     * <p>Room automatically invalidates and re-emits this list whenever
     * the underlying favorites table changes.</p>
     *
     * @return {@link LiveData} of the favorites entry list
     */
    @NonNull
    public LiveData<List<FavoriteItem>> getFavoritesListLiveData() {
        return favoritesListLiveData;
    }
}
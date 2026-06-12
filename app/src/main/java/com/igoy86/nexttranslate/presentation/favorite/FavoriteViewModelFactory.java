package com.igoy86.nexttranslate.presentation.favorite;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.igoy86.nexttranslate.domain.usecase.favorite.ClearAllFavoritesUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.DeleteFavoriteUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.GetAllFavoritesUseCase;
import com.igoy86.nexttranslate.domain.usecase.favorite.RestoreFavoriteUseCase;
import com.igoy86.nexttranslate.util.FileLogger;
 
 /**
 * Factory class responsible for creating instances of {@link FavoriteViewModel}.
 *
 * <p>Provides the required use case dependencies to {@link FavoriteViewModel}
 * while integrating with Android's {@link ViewModelProvider} API.</p>
 *
 * <p>Usage example in a Fragment:</p>
 * <pre>
 *     AppContainer container = NextTranslateApp.getContainer();
 *
 *     FavoriteViewModelFactory factory = new FavoriteViewModelFactory(
 *             container.getGetAllFavoritesUseCase(),
 *             container.getDeleteFavoriteUseCase()
 *     );
 *
 *     FavoriteViewModel viewModel = new ViewModelProvider(this, factory)
 *             .get(FavoriteViewModel.class);
 * </pre>
 */
public class FavoriteViewModelFactory implements ViewModelProvider.Factory {

    private static final String TAG = "FavoriteVMFactory";
	
	// -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs a new {@link FavoriteViewModelFactory} with all required
     * use case dependencies.
     *
     * @param getAllFavoritesUseCase use case for retrieving favorites; must not be null
     * @param deleteFavoriteUseCase use case for deleting a single entry; must not be null
     */

    @NonNull private final GetAllFavoritesUseCase getAllFavoritesUseCase;
    @NonNull private final DeleteFavoriteUseCase deleteFavoriteUseCase;
    @NonNull private final RestoreFavoriteUseCase restoreFavoriteUseCase;
    @NonNull private final ClearAllFavoritesUseCase clearAllFavoritesUseCase;

    public FavoriteViewModelFactory(
            @NonNull GetAllFavoritesUseCase getAllFavoritesUseCase,
            @NonNull DeleteFavoriteUseCase deleteFavoriteUseCase,
            @NonNull RestoreFavoriteUseCase restoreFavoriteUseCase,
            @NonNull ClearAllFavoritesUseCase clearAllFavoritesUseCase
    ) {
        this.getAllFavoritesUseCase = getAllFavoritesUseCase;
        this.deleteFavoriteUseCase = deleteFavoriteUseCase;
        this.restoreFavoriteUseCase = restoreFavoriteUseCase;
        this.clearAllFavoritesUseCase = clearAllFavoritesUseCase;
    }
	
	// -------------------------------------------------------------------------
    // ViewModelProvider.Factory implementation
    // -------------------------------------------------------------------------

    /**
     * Creates a new instance of the requested {@link ViewModel} class.
     *
     * <p>Only {@link FavoriteViewModel} is supported by this factory.
     * Requesting any other ViewModel class will throw an
     * {@link IllegalArgumentException}.</p>
     *
     * @param modelClass the class of the ViewModel to create; must not be null
     * @param <T>        the type of the ViewModel
     * @return a newly created {@link FavoriteViewModel} instance
     * @throws IllegalArgumentException if {@code modelClass} is not
     *                                  {@link FavoriteViewModel}
     */

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(FavoriteViewModel.class)) {
            FileLogger.d(TAG, "Creating FavoriteViewModel instance.");
            //noinspection unchecked
            return (T) new FavoriteViewModel(
                    getAllFavoritesUseCase,
                    deleteFavoriteUseCase,
                    restoreFavoriteUseCase,
                    clearAllFavoritesUseCase
            );
        }
        throw new IllegalArgumentException(
                "FavoriteViewModelFactory cannot create: " + modelClass.getName());
    }
}
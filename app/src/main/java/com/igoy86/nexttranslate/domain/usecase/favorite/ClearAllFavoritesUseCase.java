package com.igoy86.nexttranslate.domain.usecase.favorite;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.FavoriteRepository;

/**
 * Use case responsible for deleting all {@link com.igoy86.nexttranslate.domain.model.FavoriteItem}
 * entries from the database.
 *
 * <p>Called when the user confirms the clear-all action via the confirmation
 * dialog in {@link com.igoy86.nexttranslate.presentation.favorite.FavoriteFragment}.</p>
 */
public class ClearAllFavoritesUseCase {

    @NonNull
    private final FavoriteRepository favoriteRepository;

    public ClearAllFavoritesUseCase(@NonNull FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /** Executes the clear-all operation on a background thread via the repository. */
    public void execute() {
        favoriteRepository.clearAllFavorites();
    }
}
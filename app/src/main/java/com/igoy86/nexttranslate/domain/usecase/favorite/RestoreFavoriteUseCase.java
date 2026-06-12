package com.igoy86.nexttranslate.domain.usecase.favorite;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.domain.repository.FavoriteRepository;

/**
 * Use case responsible for restoring a previously deleted {@link FavoriteItem}
 * back into the database.
 *
 * <p>Used exclusively by the Undo action in the swipe-to-delete Snackbar
 * in {@link com.igoy86.nexttranslate.presentation.favorite.FavoriteFragment}.
 * Re-inserts the item with its original data so the entry appears exactly
 * as it was before deletion.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link FavoriteRepository} interface, not its implementation.</p>
 */
public class RestoreFavoriteUseCase {

    /** Repository used to re-insert the favorite entry. */
    @NonNull
    private final FavoriteRepository favoriteRepository;

    /**
     * Constructs a new {@link RestoreFavoriteUseCase}.
     *
     * @param favoriteRepository the repository used to restore favorites;
     *                           must not be null
     */
    public RestoreFavoriteUseCase(@NonNull FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * Re-inserts the given {@link FavoriteItem} into the database.
     *
     * <p>The operation is performed on a background disk I/O thread
     * managed by the repository implementation.</p>
     *
     * @param item the {@link FavoriteItem} to restore; must not be null
     */
    public void execute(@NonNull FavoriteItem item) {
        favoriteRepository.addFavorite(item);
    }
}
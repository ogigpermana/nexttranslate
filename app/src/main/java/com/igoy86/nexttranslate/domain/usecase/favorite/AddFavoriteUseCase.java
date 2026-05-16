package com.igoy86.nexttranslate.domain.usecase.favorite;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.domain.repository.FavoriteRepository;

/**
 * Use case responsible for adding a translation to the user's favorites.
 *
 * <p>Triggered by deliberate user action, such as tapping the bookmark
 * or star icon on a translation result.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link FavoriteRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     AddFavoriteUseCase addFavoriteUseCase = container.getAddFavoriteUseCase();
 *     addFavoriteUseCase.execute(new FavoriteItem(0, "Hello", "Halo", "en", "id", timestamp));
 * </pre>
 */
public class AddFavoriteUseCase {

    /** Repository used to persist the favorite entry. */
    @NonNull
    private final FavoriteRepository favoriteRepository;

    /**
     * Constructs a new {@link AddFavoriteUseCase} with the given repository.
     *
     * @param favoriteRepository the repository used to insert favorite entries
     */
    public AddFavoriteUseCase(@NonNull FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * Executes the use case by inserting the given {@link FavoriteItem}
     * into the database.
     *
     * <p>The actual database write is performed on a background disk I/O
     * thread managed by {@link com.igoy86.nexttranslate.util.AppExecutors}.</p>
     *
     * @param item the favorite entry to persist; must not be null
     */
    public void execute(@NonNull FavoriteItem item) {
        favoriteRepository.addFavorite(item);
    }
}
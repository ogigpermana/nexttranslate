package com.igoy86.nexttranslate.domain.usecase.favorite;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.FavoriteRepository;

/**
 * Use case responsible for deleting a single favorite translation entry
 * identified by its database ID.
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link FavoriteRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     DeleteFavoriteUseCase deleteFavoriteUseCase = container.getDeleteFavoriteUseCase();
 *     deleteFavoriteUseCase.execute(favoriteItem.getId());
 * </pre>
 */
public class DeleteFavoriteUseCase {

    /** Repository used to delete the favorite entry. */
    @NonNull
    private final FavoriteRepository favoriteRepository;

    /**
     * Constructs a new {@link DeleteFavoriteUseCase} with the given repository.
     *
     * @param favoriteRepository the repository used to delete favorite entries
     */
    public DeleteFavoriteUseCase(@NonNull FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * Executes the use case by deleting the favorite entry with the given ID.
     *
     * <p>The actual database delete is performed on a background disk I/O
     * thread managed by {@link com.igoy86.nexttranslate.util.AppExecutors}.
     * If no entry with the given ID exists, this is a no-op.</p>
     *
     * @param id the unique database ID of the favorite entry to delete
     */
    public void execute(long id) {
        favoriteRepository.deleteFavorite(id);
    }
}
package com.igoy86.nexttranslate.domain.usecase.favorite;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.FavoriteItem;
import com.igoy86.nexttranslate.domain.repository.FavoriteRepository;

import java.util.List;

/**
 * Use case responsible for retrieving all favorite translation entries.
 *
 * <p>Returns a {@link LiveData} list that is automatically updated by Room
 * whenever the favorites table changes, enabling reactive UI updates without
 * manual polling.</p>
 *
 * <p>This class belongs to the domain layer and depends only on the
 * {@link FavoriteRepository} interface, not its implementation.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     GetAllFavoritesUseCase getAllFavoritesUseCase = container.getGetAllFavoritesUseCase();
 *     getAllFavoritesUseCase.execute().observe(this, favoriteList -> {
 *         // update RecyclerView adapter
 *     });
 * </pre>
 */
public class GetAllFavoritesUseCase {

    /** Repository used to retrieve favorite entries. */
    @NonNull
    private final FavoriteRepository favoriteRepository;

    /**
     * Constructs a new {@link GetAllFavoritesUseCase} with the given repository.
     *
     * @param favoriteRepository the repository used to fetch favorite entries
     */
    public GetAllFavoritesUseCase(@NonNull FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * Executes the use case by returning a {@link LiveData} list of all
     * favorite entries ordered from newest to oldest.
     *
     * @return a {@link LiveData} emitting the full list of {@link FavoriteItem} objects
     */
    @NonNull
    public LiveData<List<FavoriteItem>> execute() {
        return favoriteRepository.getAllFavorites();
    }
}
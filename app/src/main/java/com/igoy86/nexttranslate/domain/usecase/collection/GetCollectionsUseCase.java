package com.igoy86.nexttranslate.domain.usecase.collection;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.igoy86.nexttranslate.domain.model.CollectionItem;
import com.igoy86.nexttranslate.domain.repository.CollectionRepository;

import java.util.List;

/**
 * Use case for retrieving all user collections as a reactive {@link LiveData} stream.
 *
 * <p>This use case belongs to the domain layer. It encapsulates the business
 * rule of fetching all collections and delegates to {@link CollectionRepository}.</p>
 *
 * <p>Instantiated and provided by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class GetCollectionsUseCase {

    @NonNull
    private final CollectionRepository collectionRepository;

    /**
     * Constructs a new {@link GetCollectionsUseCase}.
     *
     * @param collectionRepository the repository for collection operations; must not be null
     */
    public GetCollectionsUseCase(@NonNull CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    /**
     * Returns a {@link LiveData} list of all user collections ordered
     * from newest to oldest.
     *
     * @return a {@link LiveData} emitting the full list of {@link CollectionItem} objects
     */
    public LiveData<List<CollectionItem>> execute() {
        return collectionRepository.getAllCollections();
    }
}

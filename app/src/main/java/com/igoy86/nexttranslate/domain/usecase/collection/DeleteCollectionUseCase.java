package com.igoy86.nexttranslate.domain.usecase.collection;

import androidx.annotation.NonNull;

import com.igoy86.nexttranslate.domain.repository.CollectionRepository;

/**
 * Use case for deleting a user collection by its database ID.
 *
 * <p>This use case belongs to the domain layer. It encapsulates the business
 * rule of deleting a collection and delegates to {@link CollectionRepository}.</p>
 *
 * <p>Instantiated and provided by
 * {@link com.igoy86.nexttranslate.di.AppContainer}.</p>
 */
public class DeleteCollectionUseCase {

    @NonNull
    private final CollectionRepository collectionRepository;

    /**
     * Constructs a new {@link DeleteCollectionUseCase}.
     *
     * @param collectionRepository the repository for collection operations; must not be null
     */
    public DeleteCollectionUseCase(@NonNull CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    /**
     * Executes the collection deletion.
     *
     * <p>Dispatched on a background disk I/O thread. Fire-and-forget.</p>
     *
     * @param collectionId the unique database ID of the collection to delete
     */
    public void execute(long collectionId) {
        collectionRepository.deleteCollection(collectionId);
    }
}
